package com.sohu.mrd

import com.sohu.mrd.framework.redis.client.CodisRedisClient
import org.apache.spark.broadcast.Broadcast
import org.apache.spark.mllib.recommendation.{ALS, MatrixFactorizationModel, Rating}
import org.apache.spark.rdd.RDD
import org.apache.spark.{SparkConf, SparkContext}
import redis.clients.jedis.JedisPoolConfig

import scala.collection.Map
import scala.collection.mutable.ArrayBuffer
import scala.util.Random

/**
  * Created by yonghongli on 2017/3/16.
  */
object RunALSRecommender {

  def main(args: Array[String]): Unit = {
    val conf = new SparkConf().setAppName("RunALSRecommender")
    val sc = new SparkContext(conf)

    val base = "hdfs://heracles/user/mrd/novel_user_behivor/"+args.apply(args.length-1)+"/"
    println(base)
    val rawUserNovelData = sc.textFile(base,50)
    val userNovel = rawUserNovelData.map { line =>
      val Array(_,user, novel, _*) = line.split('\t')
      (user, novel)
    }.repartition(50)
    val userArtistCount = userNovel.map(x=>((x._1,x._2),1)).reduceByKey((x,y)=>x+y).map{case (x,y)=>(x._1,(x._2,y))}
    val userMapInt = userArtistCount.map(x=>x._1).distinct().zipWithIndex().map(x=>(x._1,x._2.toInt))
    val intMapUser = userMapInt.map(x =>(x._2,x._1))
    println("user total  count is :"+userMapInt.count())
    val UserindexNovelIdCountsRating = userMapInt.join(userArtistCount).map{
      case (user,(userIndex,(novel,count)))=>
        Rating(userIndex,novel.toInt,count)
    }.repartition(50)

   // preparation(UserindexNovelIdCountsRating)
   // model(sc,UserindexNovelIdCountsRating)
   // evaluate(sc,UserindexNovelIdCountsRating)
    recommend(sc,UserindexNovelIdCountsRating,intMapUser)
  }



  def preparation(rawUserArtistData: RDD[Rating]) = {
    val userIDStats = rawUserArtistData.map(r=>r.user.toDouble).stats()
    val itemIDStats = rawUserArtistData.map(r=>r.product.toDouble).stats()
    println(userIDStats)
    println(itemIDStats)
  }

  def buildRatings(sc:SparkContext,
                    rawUserArtistData: RDD[((String,String),Int)],
                    useridIndexAlias: Map[String,Int]) = {
    val bUseridToIndex = sc.broadcast(useridIndexAlias)
    rawUserArtistData.map {
      case (((userID, novelId), count)) =>
      val finalUserId = bUseridToIndex.value.get(userID)
      Rating(finalUserId.get, novelId.toInt, count)
    }
  }

  def model(
             sc: SparkContext,
             UserindexNovelIdCountsRating: RDD[Rating]): Unit = {


   val  trainData =UserindexNovelIdCountsRating.cache()

    val model = ALS.trainImplicit(trainData, 20, 10, 25.0, 25.0)

    trainData.unpersist()

    println(model.userFeatures.mapValues(_.mkString(", ")).first())

    val userID = 100
    val recommendations = model.recommendProducts(userID, 5)
    recommendations.foreach(println)
    val recommendedProductIDs = recommendations.map(_.product).toSet

    val rawNovelsForUser = UserindexNovelIdCountsRating.
      filter { case f => f.user == userID }

    val existingProducts = rawNovelsForUser.map { case f => f.product }.
      collect().toSet
    recommendedProductIDs.foreach(println)
    unpersist(model)
  }

  def areaUnderCurve(
                      positiveData: RDD[Rating],
                      bAllItemIDs: Broadcast[Array[Int]],
                      predictFunction: (RDD[(Int,Int)] => RDD[Rating])) = {
    // What this actually computes is AUC, per user. The result is actually something
    // that might be called "mean AUC".

    // Take held-out data as the "positive", and map to tuples
    val positiveUserProducts = positiveData.map(r => (r.user, r.product))
    // Make predictions for each of them, including a numeric score, and gather by user
    val positivePredictions = predictFunction(positiveUserProducts).groupBy(_.user)

    // BinaryClassificationMetrics.areaUnderROC is not used here since there are really lots of
    // small AUC problems, and it would be inefficient, when a direct computation is available.

    // Create a set of "negative" products for each user. These are randomly chosen
    // from among all of the other items, excluding those that are "positive" for the user.
    val negativeUserProducts = positiveUserProducts.groupByKey().mapPartitions {
      // mapPartitions operates on many (user,positive-items) pairs at once
      userIDAndPosItemIDs => {
        // Init an RNG and the item IDs set once for partition
        val random = new Random()
        val allItemIDs = bAllItemIDs.value
        userIDAndPosItemIDs.map { case (userID, posItemIDs) =>
          val posItemIDSet = posItemIDs.toSet
          val negative = new ArrayBuffer[Int]()
          var i = 0
          // Keep about as many negative examples per user as positive.
          // Duplicates are OK
          while (i < allItemIDs.size && negative.size < posItemIDSet.size) {
            val itemID = allItemIDs(random.nextInt(allItemIDs.size))
            if (!posItemIDSet.contains(itemID)) negative += itemID
            i += 1
          }
          // Result is a collection of (user,negative-item) tuples
          negative.map(itemID => (userID, itemID))
        }
      }
    }.flatMap(t => t)
    // flatMap breaks the collections above down into one big set of tuples

    // Make predictions on the rest:
    val negativePredictions = predictFunction(negativeUserProducts).groupBy(_.user)

    // Join positive and negative by user
    positivePredictions.join(negativePredictions).values.map {
      case (positiveRatings, negativeRatings) =>
        // AUC may be viewed as the probability that a random positive item scores
        // higher than a random negative one. Here the proportion of all positive-negative
        // pairs that are correctly ranked is computed. The result is equal to the AUC metric.
        var correct = 0L
        var total = 0L
        // For each pairing,
        for (positive <- positiveRatings;
             negative <- negativeRatings) {
          // Count the correctly-ranked pairs
          if (positive.rating > negative.rating) correct += 1
          total += 1
        }
        // Return AUC: fraction of pairs ranked correctly
        correct.toDouble / total
    }.mean() // Return mean AUC over users
  }

  def predictMostViewed(sc: SparkContext, train: RDD[Rating])(allData: RDD[(Int,Int)]) = {
    val bListenCount =
      sc.broadcast(train.map(r => (r.product, r.rating)).reduceByKey(_ + _).collectAsMap())
    allData.map { case (user, product) =>
      Rating(user, product, bListenCount.value.getOrElse(product, 0.0))
    }
  }

  def evaluate(
                sc: SparkContext,
                UserindexNovelIdCountsRating: RDD[Rating]): Unit = {


    val allData = UserindexNovelIdCountsRating
    val Array(trainData, cvData) = allData.randomSplit(Array(0.9, 0.1))
    trainData.repartition(50).cache()
    cvData.repartition(50).cache()

    val allItemIDs = allData.map(_.product).distinct().collect()
    val bAllItemIDs = sc.broadcast(allItemIDs)

    val mostListenedAUC = areaUnderCurve(cvData, bAllItemIDs, predictMostViewed(sc, trainData))
    println(mostListenedAUC)

    val evaluations =
      for (rank   <- Array(20,  22);
           lambda <- Array(25, 30);
           alpha  <- Array(25, 30))
        yield {
          val model = ALS.trainImplicit(trainData, rank, 10, lambda, alpha)
          val auc = areaUnderCurve(cvData, bAllItemIDs, model.predict)
          unpersist(model)
          ((rank, lambda, alpha), auc)
        }

    evaluations.sortBy(_._2).reverse.foreach(println)

    trainData.unpersist()
    cvData.unpersist()
  }

  def recommend(
                 sc: SparkContext, UserindexNovelIdCountsRating: RDD[Rating],intMapUser:RDD[(Int,String)]): Unit = {

    val allData = UserindexNovelIdCountsRating.cache()
    val model: MatrixFactorizationModel = ALS.trainImplicit(allData, 20, 10, 25.0, 25.0)
    allData.unpersist()
    val userID = 100
    val recommendations= model.recommendProducts(userID, 5)
    val recommendedProductIDs = recommendations.map(_.product).toSet
    recommendedProductIDs.foreach(println)
    val allUsers = allData.map(_.user).distinct()


    val allRecommendations: RDD[(Int, Array[Rating])] = model.recommendProductsForUsers(10);

    val userAndRecommendations=allRecommendations.map(
      recs => (recs._1, recs._2.map(_.product).mkString(","))
    )

    val userAndrecommendProducts =intMapUser.join(userAndRecommendations).map{
      case (userIndex,(user,novelIDs))=>
        (user,novelIDs)
    }

    userAndrecommendProducts.foreachPartition {case x=>
      //数据放到redis

      val config = new JedisPoolConfig
      config.setMinEvictableIdleTimeMillis(60000)
      config.setTimeBetweenEvictionRunsMillis(30000)
      val jedisPool = CodisRedisClient.getJedisPool("mrd_redis_1", config, 0, "test")
      val key = CodisRedisClient.mkKey("ALS_novel_cid", "ALS-novel-recom")

        x.foreach{
          case (user, pids) =>
            val jedis = jedisPool.getResource
            val l = jedis.hset(key.toString, user.toString, pids.toString)
            jedis.expire(key.toString, 2 * 24 * 60 * 60)
            jedis.close()
        }

    }


    unpersist(model)
  }

  def unpersist(model: MatrixFactorizationModel): Unit = {
    // At the moment, it's necessary to manually unpersist the RDDs inside the model
    // when done with it in order to make sure they are promptly uncached
    model.userFeatures.unpersist()
    model.productFeatures.unpersist()
  }

}



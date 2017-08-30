package com.sohu.mrd.hotnovelrank.bolt.kafka;
import com.sohu.mrd.framework.redis.client.CodisRedisClient;
import com.sohu.mrd.framework.redis.client.codis.JedisResourcePool;
import org.apache.storm.task.TopologyContext;
import org.apache.storm.topology.BasicOutputCollector;
import org.apache.storm.topology.OutputFieldsDeclarer;
import org.apache.storm.topology.base.BaseBasicBolt;
import org.apache.storm.tuple.Tuple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPoolConfig;

import java.util.Map;

/**
 * Created by yonghongli on 2016/7/18.
*/
public class NovelCountBolt extends BaseBasicBolt {
    private static final Logger LOG = LoggerFactory.getLogger(NovelCountBolt.class);

    public static JedisResourcePool jedisPool = null;
    public Jedis jedis=null;
    private static final String clusterName = "mrd_redis_1";
    @Override
    public void prepare(Map stormConf, TopologyContext context ) {

        super.prepare(stormConf, context);
        initialJedisPool();
    }

    @Override
    public void declareOutputFields(OutputFieldsDeclarer outputFieldsDeclarer) {
       // outputFieldsDeclarer.declare(new Fields("keyword", "count"));
    }

    @Override
    public void execute(Tuple tuple, BasicOutputCollector collector) {
   try{
        String userBehavior = tuple.getStringByField("sentence");
        LOG.info("userBehavior....."+userBehavior);
        String[] s = userBehavior.split("\t");
        if(s.length>=3) {
            String date = s[0].split(" ")[0];
            String cid = s[1];
            String oid = s[2];
            String key = CodisRedisClient.mkKey("hot_novel_" + date, "hot-novel-recom");


            jedis = jedisPool.getResource();
            long l = jedis.hincrBy(key, oid, 1);
            jedis.expire(key, 2 * 24 * 60 * 60);
        }
        }catch(Exception e){
       LOG.info(e.getMessage());
        }finally {
            //回收jedis实例
            if (jedis != null){
                jedis.close();
            }
        }



    }

    @Override
    public void cleanup() {
        if (jedis != null){
            jedis.close();
        }
        super.cleanup();
    }

    private static void initialJedisPool()
    {


        JedisPoolConfig config = new JedisPoolConfig();

        config.setMinEvictableIdleTimeMillis(60000);

        config.setTimeBetweenEvictionRunsMillis(30000);


        jedisPool = CodisRedisClient.getJedisPool(clusterName, config, 0,"test");

    }
}
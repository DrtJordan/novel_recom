package com.sohu.mrd.util;

import com.sohu.mrd.framework.redis.client.CodisRedisClient;
import com.sohu.mrd.framework.redis.client.codis.JedisResourcePool;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPoolConfig;

import java.text.ParseException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Created by yonghongli on 2016/8/3.
 */
public class GetDataFromRedis {

    private static JedisResourcePool jedisPool = null;
    private static Jedis jedis = null;
    private static final String clusterName = "mrd_redis_1";

    static {
        JedisPoolConfig config = new JedisPoolConfig();

        config.setMinEvictableIdleTimeMillis(60000);

        config.setTimeBetweenEvictionRunsMillis(30000);
          jedisPool = CodisRedisClient.getJedisPool(clusterName, config, 0, "test");


    }

    public static Map<String, Integer> getRedisDate(String time, int topN) {
        Map<String, Integer> result = new LinkedHashMap<String, Integer>();
        try {
            jedis = jedisPool.getResource();
            String key = CodisRedisClient.mkKey("hotword_" + time, "search-hot-word");
            Map<String, String> h = jedis.hgetAll(key);
            if (h == null) {
                return null;
            }
            SimpleTopNTool utl = new SimpleTopNTool(topN);
            for (Map.Entry<String, String> s : h.entrySet()) {
                utl.addElement(new SimpleTopNTool.SortElement(Integer.valueOf(s.getValue()), s.getKey()));
            }

            for (SimpleTopNTool.SortElement ele : utl.getTopN()) {
                result.put(ele.getVal().toString().replace(",","").replaceAll("[\\pP‘’“”| \t|　]", ""), (int) ele.getCount());
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            jedis.close();
        }
        return result;

    }


    public static Map<String, String> getRedisDate( ) {
        Map<String, String> h =new HashMap<>();
        try {
            jedis = jedisPool.getResource();

            String key = CodisRedisClient.mkKey("ALS_novel_cid", "ALS-novel-recom");

            h = jedis.hgetAll(key);
            if (h == null) {
                return h;
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            jedis.close();
        }
        return h;

    }

    public static void main(String[] args) throws ParseException {
        //  System.out.println(DateUtils.getDateTimeList("2016-12-0818", "2016-12-0819"));
     // Map<String, Integer> h = GetDataFromRedis.getRedisDate("2017-03-1611", 10);

         Map<String, String > h = GetDataFromRedis.getRedisDate();
        System.out.println(h.size());
         int i=0;
        for (Map.Entry entry:h.entrySet()){
            System.out.println(entry.getKey()+"--->"+entry.getValue());
            i++;
//            if(i==9){
//                break;
//            }
        }

        //  System.out.println(h.keySet());

//        for (Map.Entry<String, Integer> kw : h.entrySet()) {
//            System.out.println(kw.getKey() + ":" + kw.getValue());
//        }
    }



}


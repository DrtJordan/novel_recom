package com.sohu.mrd.service.impl;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.sohu.mrd.constant.Constant;

import com.sohu.mrd.factory.HttpClientFactory;
import com.sohu.mrd.framework.redis.client.CodisRedisClient;
import com.sohu.mrd.framework.redis.client.codis.JedisResourcePool;
import com.sohu.mrd.service.NovelRecomService;
import com.sohu.mrd.util.ConfigUtil;
import com.sohu.mrd.util.SimpleTopNTool;

import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPoolConfig;

import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Created by yonghongli on 2017/3/10.
 */
@Service
public class NovelRecomServiceImp implements NovelRecomService {

    private static final Logger logger = LogManager.getLogger(NovelRecomServiceImp.class);
    private static JedisResourcePool jedisPool = null;
    private static Jedis jedis=null;
    private static final String clusterName = "mrd_redis_1";
    private  static SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");


    private static PoolingHttpClientConnectionManager cm;

    static {
        JedisPoolConfig config = new JedisPoolConfig();

        config.setMinEvictableIdleTimeMillis(60000);

        config.setTimeBetweenEvictionRunsMillis(30000);
        jedisPool = CodisRedisClient.getJedisPool(clusterName, config, 0, "test");


    }


    @Override
    public JSONArray getNovelRecom(String cid, String pid) {

        List<String> temp = new ArrayList<>();
        JSONArray resultJsonArray= new JSONArray();
        String date = sdf.format(System.currentTimeMillis());
        if (cid.equals("0") && pid.equals("0")) {
            temp= getTopNNovel(date,10);
            if(temp.size()==0){
                 date = sdf.format(System.currentTimeMillis()-24*60*60*1000);
                temp= getTopNNovel(date,10);
            }
            if(temp.size()!=0){
                for(String nid:temp){
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("id",nid);
                    resultJsonArray.add(jsonObject);
                }
            }else {
                resultJsonArray=getJsonArrayFromES();
            }
            logger.info("[getNovelRecom] cid:{},,pid:{} result:{}", cid,pid ,resultJsonArray.toString());

            return resultJsonArray;
        } else {
            //获取算法结果
            temp.addAll(getASLRecomNovel(cid));

            logger.info("[getNovelRecom] By algorithm  cid:{},pid:{} size:{}", cid,pid ,temp.size());

            //获取热小说结果
            temp.addAll(getTopNNovel(date,10));
            logger.info("[getNovelRecom] By algorithm  and hot novel cid:{},pid:{} size:{}", cid,pid ,temp.size());

            //去重
            HashSet<String>  tempH= new HashSet<>(temp);
            temp = new ArrayList<>(tempH);
            if(temp.size()>=10){
                for (int i=0;i<10;i++){
                    Random random = new Random();
                    int novelIndex = random.nextInt(temp.size());
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("id",temp.get(novelIndex));
                    resultJsonArray.add(jsonObject);
                    temp.remove(novelIndex);
                }
                logger.info("[getNovelRecom] By algorithm or hot novel and  select cid:{},pid:{} result:{}", cid,pid ,resultJsonArray.toString());

                return resultJsonArray;
            }else {
                if(temp.size()>0){
                    for(String nid:temp){
                        JSONObject jsonObject = new JSONObject();
                        jsonObject.put("id",nid);
                        resultJsonArray.add(jsonObject);
                    }
                    logger.info("[getNovelRecom] By algorithm or hot novel  cid:{},,pid:{} result:{}", cid,pid ,resultJsonArray.toString());
                    return resultJsonArray;
                }else {
                    //随机从elasticsearch获取，为上面取不到小说做灾备
                    resultJsonArray=getJsonArrayFromES();
                    logger.info("[getNovelRecom] By ES  cid:{},,pid:{} result:{}", cid,pid ,resultJsonArray.toString());

                    logger.info("By ES cid:"+cid+",:pid"+pid+",result="+resultJsonArray);
                    return resultJsonArray;
                }

            }


        }
    }



    private List<String> getASLRecomNovel(String cid) {
        List<String> result=new ArrayList<>();
        try {
            jedis = jedisPool.getResource();
            String key = CodisRedisClient.mkKey("ALS_novel_cid", "ALS-novel-recom");
            String  nids = jedis.hget(key,cid);
            if (nids == null) {
                return result;
            }else{
                result= new ArrayList<>(Arrays.asList(nids.split(",",-1)));
            }


        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            jedis.close();
        }

        return result;

    }

    private JSONArray getJsonArrayFromES() {
        String url = ConfigUtil.configProperties.get(Constant.ES_RANDOM_URL).toString();
        JSONArray jsonArray = new JSONArray();
        try {
            CloseableHttpClient httpclient = HttpClientFactory.getHttpClient();
            HttpGet httpget = new HttpGet(url);
            logger.info("executing request " + httpget.getURI());

            long start = System.currentTimeMillis();
            CloseableHttpResponse response = httpclient.execute(httpget);
            long end = System.currentTimeMillis();
            logger.info("response cost:" + (end - start) + " ms");
            try {
                HttpEntity entity = response.getEntity();
                if(response.getStatusLine().getStatusCode() == HttpStatus.SC_OK){
                    if (entity != null) {
                        String entityStr = EntityUtils.toString(entity);
                        JSONObject   jsonObjectFromES = JSONObject.parseObject(entityStr);
                        if(jsonObjectFromES.get("status").equals("C0000")){
                            JSONArray items = jsonObjectFromES.getJSONArray("items");
                            Iterator<Object> it = items.iterator();
                            while (it.hasNext()) {
                                JSONObject jsonObjectOfItems = (JSONObject) it.next();
                                JSONObject jsonObject = new JSONObject();
                                jsonObject.put("id",jsonObjectOfItems.get("newsid"));
                                jsonArray.add(jsonObject);
                            }

                        }


                    }
                }
            } finally {
                response.close();
            }

        } catch (Exception e) {
            logger.error(e.getMessage(),e);
        }
        return jsonArray;
    }


    public static List<String> getTopNNovel(String date ,int topN) {
        Map<String, Integer> temp = new LinkedHashMap<String, Integer>();
        List<String> result = new ArrayList<>();
        try {
            jedis = jedisPool.getResource();
            String key = CodisRedisClient.mkKey("hot_novel_" + date, "hot-novel-recom");
            Map<String, String> h = jedis.hgetAll(key);
            if (h == null||h.size()==0) {
                return result;
            }
            SimpleTopNTool utl = new SimpleTopNTool(topN);
            for (Map.Entry<String, String> s : h.entrySet()) {
                utl.addElement(new SimpleTopNTool.SortElement(Integer.valueOf(s.getValue()), s.getKey()));
            }

            for (SimpleTopNTool.SortElement ele : utl.getTopN()) {
                temp.put(ele.getVal().toString(), (int) ele.getCount());
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            jedis.close();
        }
        result.addAll(temp.keySet());
        return result;

    }


}

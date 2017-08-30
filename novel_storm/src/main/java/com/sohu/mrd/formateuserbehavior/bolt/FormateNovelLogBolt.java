/**
 * 
 */
package com.sohu.mrd.formateuserbehavior.bolt;

import java.text.SimpleDateFormat;
import java.util.HashSet;
import java.util.Iterator;

import com.sohu.mrd.formateuserbehavior.constant.KafkaConstant;
import com.sohu.mrd.util.KafkaKit;
import org.apache.storm.topology.BasicOutputCollector;
import org.apache.storm.topology.OutputFieldsDeclarer;
import org.apache.storm.topology.base.BaseBasicBolt;
import org.apache.storm.tuple.Tuple;

import com.alibaba.fastjson.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by yonghongli on 2017/3/11.
 */
public class FormateNovelLogBolt extends BaseBasicBolt {
	private static final Logger LOG = LoggerFactory.getLogger(FormateNovelLogBolt.class);
	SimpleDateFormat format= new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	/* (non-Javadoc)
	 * @see org.apache.storm.topology.IBasicBolt#execute(org.apache.storm.tuple.Tuple, org.apache.storm.topology.BasicOutputCollector)
	 */
	public void execute(Tuple input, BasicOutputCollector collector) {
		// TODO Auto-generated method stub
		try {
			String msg=input.getStringByField("msg");
			if(msg.contains("channelId\":\"960415\""))
			{
				JSONObject  jsonObject=JSONObject.parseObject(msg);
				String ts=jsonObject.getString("ts");
				String date=format.format(Long.parseLong(ts)*1000);
				String newsIds=jsonObject.getString("newsids");
				String[] newsIdArray=newsIds.split(",", -1);
                HashSet<String> set=new HashSet<String>(); 
				for(int i=0;i<newsIdArray.length;i++)
				{
					String oid=newsIdArray[0].split("_", -1)[1];
					set.add(oid);
				}
				Iterator<String> it=set.iterator();
				StringBuilder sb = new StringBuilder();
				while(it.hasNext())
				{
					String id=it.next();
					sb.append(id);
					sb.append(",");
				}
				sb.delete(sb.length()-1,sb.length()+1);
				String oids=sb.toString();
				String act=jsonObject.getString("act");
				String cid=jsonObject.getString("cid");
				String channelId=jsonObject.getString("channelId");
				String sendKafkaMsg=date+"\t"+cid+"\t"+oids+"\t"+act;
				LOG.info("向kafka发送的数据 "+sendKafkaMsg);LOG.info("向kafka发送的数据 "+sendKafkaMsg);
			    KafkaKit kafkaKit=new KafkaKit();
			    kafkaKit.sendMessage(KafkaConstant.FORMATE_USER_BEHAVIOR_TOPIC, sendKafkaMsg);
			}
		} catch (Exception e) {
			LOG.equals("解析msg数据异常");
		}
		
	}

	/* (non-Javadoc)
	 * @see org.apache.storm.topology.IComponent#declareOutputFields(org.apache.storm.topology.OutputFieldsDeclarer)
	 */
	public void declareOutputFields(OutputFieldsDeclarer declarer) {
		// TODO Auto-generated method stub

	}

}

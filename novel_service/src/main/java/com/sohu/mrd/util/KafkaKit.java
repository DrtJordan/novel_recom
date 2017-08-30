package com.sohu.mrd.util;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.log4j.Logger;

import kafka.consumer.Consumer;
import kafka.consumer.ConsumerConfig;
import kafka.consumer.ConsumerIterator;
import kafka.consumer.KafkaStream;
import kafka.javaapi.consumer.ConsumerConnector;
import kafka.message.MessageAndMetadata;

import com.google.common.base.Function;
/**
 * @author Jin Guopan
   @creation 2016年12月20日
 */
public class KafkaKit {
	private static final Logger LOG = Logger.getLogger(KafkaKit.class);
	private Properties getConsumerProperties(final String groupId)
	{
		Properties props = new Properties();
		props.put("group.id", groupId);
		props.put("zookeeper.connect", "10.10.91.91:2181,10.10.91.92:2181,10.10.91.93:2181");
		props.put("auto.offset.reset", "largest");
		props.put("auto.commit.interval.ms", "1000");
		props.put("partition.assignment.strategy", "roundrobin");
		return props;
	}
	/**
	 * 消费kafka数据 final 修饰防止传入的变量被修改
	 * @param groupId
	 * @param topic
	 * @param messageHandler
	 */
	public void  consumerMessage(final String groupId,final String topic,Integer threadNum,final LinkedBlockingQueue<String> queue)
	{
		Properties props=getConsumerProperties(groupId);
		 ConsumerConfig config = new ConsumerConfig(props);
		 ConsumerConnector consumerConnector=Consumer.createJavaConsumerConnector(config);
		 Map<String,Integer> ConsumerCountMap = new HashMap<String,Integer>();
		 ConsumerCountMap.put(topic, threadNum);
		 Map<String, List<KafkaStream<byte[], byte[]>>>  topicStreamMap=consumerConnector.createMessageStreams(ConsumerCountMap);
		 List<KafkaStream<byte[], byte[]>>  streamList= topicStreamMap.get(topic);
		 ExecutorService  excutor= Executors.newFixedThreadPool(threadNum);
		 final AtomicInteger countConsumer = new AtomicInteger();
		 for(int i=0;i<streamList.size();i++)
		 {
			 final KafkaStream<byte[], byte[]> stream = streamList.get(i); //每个消费流
			 //每个消费流使用一个线程进行消费
			 excutor.submit(new Runnable() {
				public void run() {
					 ConsumerIterator<byte[], byte[]> it=stream.iterator();
					 while(it.hasNext())
					 {
						 //LOG.info("总共消费了 "+countConsumer.incrementAndGet()+" 个");
						 MessageAndMetadata<byte[], byte[]>  data= it.next();
						 byte[]  message = data.message();
						 String msg=new String(message);
						 queue.offer(msg);
						 LOG.info("msg "+msg);
					 }
				}
			});
		 }
	}
	
	public static void main(String[] args) {
		LinkedBlockingQueue<String> queue = new LinkedBlockingQueue<String>();
		KafkaKit kit=new KafkaKit();
		//com_sohu_mrd_text_classification_sexy_ads_logs_
		//kit.consumerMessage("com_sohu_mrd_online_docId_sexy_ads_logs_consumer", "com_sohu_mrd_online_docId_sexy_ads_logs", 3, queue);
		
		//kit.consumerMessage("testn_test_pan1238", "com_sohu_mrd_text_classification_sexy_ads_logs_", 3, queue);
		
		 
		
		kit.consumerMessage("testn_te333st_pan1238", "madc1_mrd", 3, queue);

	}
	
}

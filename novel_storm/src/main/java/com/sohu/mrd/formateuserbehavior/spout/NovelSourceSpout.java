/**
 * 
 */
package com.sohu.mrd.formateuserbehavior.spout;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;

import com.sohu.mrd.formateuserbehavior.constant.KafkaConstant;
import com.sohu.mrd.util.KafkaKit;
import org.apache.storm.spout.SpoutOutputCollector;
import org.apache.storm.task.TopologyContext;
import org.apache.storm.topology.OutputFieldsDeclarer;
import org.apache.storm.topology.base.BaseRichSpout;
import org.apache.storm.tuple.Fields;
import org.apache.storm.tuple.Values;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by yonghongli on 2017/3/11.
   读取行为日志
 */
public class NovelSourceSpout extends BaseRichSpout {
	private static final Logger LOG = LoggerFactory.getLogger(NovelSourceSpout.class);
	/* (non-Javadoc)
	 * @see org.apache.storm.spout.ISpout#open(java.util.Map, org.apache.storm.task.TopologyContext, org.apache.storm.spout.SpoutOutputCollector)
	 */
	LinkedBlockingQueue<String> queue;
	SpoutOutputCollector collector;
	public void open(Map conf, TopologyContext context,
			SpoutOutputCollector collector) {
		this.collector=collector;
	}
	/* (non-Javadoc)
	 * @see org.apache.storm.spout.ISpout#close()
	 */
	public void close() {
		// TODO Auto-generated method stub
	}

	/* (non-Javadoc)
	 * @see org.apache.storm.spout.ISpout#activate()
	 */
	public void activate() {
		queue=new LinkedBlockingQueue<String>();
		KafkaKit kafkaKit=new KafkaKit();
		kafkaKit.consumerMessage(KafkaConstant.USER_BEHAVIOR_CONSUMER_GROUP, KafkaConstant.USER_BEHAVIOR_TOPIC, 3, queue);
	}

	/* (non-Javadoc)
	 * @see org.apache.storm.spout.ISpout#deactivate()
	 */
	public void deactivate() {
		// TODO Auto-generated method stub
	}
	/* (non-Javadoc)
	 * @see org.apache.storm.spout.ISpout#nextTuple()
	 */
	public void nextTuple() {
		if(!queue.isEmpty())
		{
			String msg=queue.poll();
			collector.emit(new Values(msg));
		}
	}

	/* (non-Javadoc)
	 * @see org.apache.storm.spout.ISpout#ack(java.lang.Object)
	 */
	public void ack(Object msgId) {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see org.apache.storm.spout.ISpout#fail(java.lang.Object)
	 */
	public void fail(Object msgId) {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see org.apache.storm.topology.IComponent#declareOutputFields(org.apache.storm.topology.OutputFieldsDeclarer)
	 */
	public void declareOutputFields(OutputFieldsDeclarer declarer) {
		declarer.declare(new Fields("msg"));
	}

	/* (non-Javadoc)
	 * @see org.apache.storm.topology.IComponent#getComponentConfiguration()
	 */
	public Map<String, Object> getComponentConfiguration() {
		// TODO Auto-generated method stub
		return null;
	}

}

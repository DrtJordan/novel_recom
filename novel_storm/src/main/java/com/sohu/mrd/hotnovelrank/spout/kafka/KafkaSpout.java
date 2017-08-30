package com.sohu.mrd.hotnovelrank.spout.kafka;

import com.sohu.mrd.util.KafkaKit;
import com.sohu.smc.common.kafka.Kafka;
import org.apache.storm.spout.SpoutOutputCollector;
import org.apache.storm.task.TopologyContext;
import org.apache.storm.topology.IRichSpout;
import org.apache.storm.topology.OutputFieldsDeclarer;
import org.apache.storm.tuple.Fields;
import org.apache.storm.tuple.Values;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created by yonghongli on 2017/3/11.
 */
public class KafkaSpout implements IRichSpout {
    private static final Logger LOG = LoggerFactory.getLogger(KafkaSpout.class);

    private SpoutOutputCollector collector;
    private    Kafka kafka;
    private String topic;
    private LinkedBlockingQueue<String> quene;
    private ConcurrentHashMap<UUID, Values> pending;

    private int count = 0;


    public KafkaSpout() {
        super();
    }

    public KafkaSpout(String topic){
        this.topic = topic;
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return super.equals(obj);
    }

    @Override
    protected Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

    @Override
    public String toString() {
        return super.toString();
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
    }

    public void open(Map config, TopologyContext context,
                     SpoutOutputCollector collector) {
        this.collector = collector;
        this.pending = new ConcurrentHashMap<UUID, Values>();
    }

    public void close() {

    }


    public void activate() {
        quene = new LinkedBlockingQueue<String>();
        KafkaKit kit=new KafkaKit();
        kit.consumerMessage("top_novel_storm", topic, 3, quene);
    }

    public void deactivate() {

    }

    public void nextTuple() {
        while(!quene.isEmpty()){
            String msg  = quene.poll();

            Values values = new Values(msg, System.currentTimeMillis());
            UUID msgId = UUID.randomUUID();
            this.pending.put(msgId, values);
            this.collector.emit(values, msgId);

            count++;
            if(count > 20000){
                count = 0;
                System.out.println("Pending count: " + this.pending.size());
            }
            Thread.yield();
        }
    }

    public void ack(Object msgId) {
        this.pending.remove(msgId);
    }

    public void fail(Object msgId) {
        System.out.println("**** RESENDING FAILED TUPLE");
        this.collector.emit(this.pending.get(msgId), msgId);
    }

    public void declareOutputFields(OutputFieldsDeclarer declarer) {
        declarer.declare(new Fields("sentence", "timestamp"));
    }

    public Map<String, Object> getComponentConfiguration() {
        topic = "com_sohu_mrd_user_behavior_novel_2";
        return null;
    }

}

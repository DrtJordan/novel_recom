package com.sohu.mrd.hotnovelrank.bolt.kafka;

import com.sohu.mrd.framework.redis.client.CodisRedisClient;
import com.sohu.mrd.framework.redis.client.codis.JedisResourcePool;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.SequenceFile;
import org.apache.hadoop.io.compress.CompressionCodecFactory;
import org.apache.storm.hdfs.bolt.AbstractHdfsBolt;
import org.apache.storm.hdfs.bolt.format.FileNameFormat;
import org.apache.storm.hdfs.bolt.format.SequenceFormat;
import org.apache.storm.hdfs.bolt.rotation.FileRotationPolicy;
import org.apache.storm.hdfs.bolt.sync.SyncPolicy;
import org.apache.storm.hdfs.common.rotation.RotationAction;
import org.apache.storm.task.OutputCollector;
import org.apache.storm.task.TopologyContext;
import org.apache.storm.topology.OutputFieldsDeclarer;
import org.apache.storm.tuple.Tuple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPoolConfig;

import java.io.IOException;
import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.Map;

/**
 * Created by yonghongli on 2017/3/11.
*/
public class NovelToHdfsBolt extends AbstractHdfsBolt {
    private static final Logger LOG = LoggerFactory.getLogger(NovelToHdfsBolt.class);

    public static JedisResourcePool jedisPool = null;
    public Jedis jedis=null;
    private static final String clusterName = "mrd_redis_1";
    static SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

    private SequenceFormat format;
    private SequenceFile.CompressionType compressionType = SequenceFile.CompressionType.RECORD;
    private transient SequenceFile.Writer writer;

    private String compressionCodec = "default";
    private transient CompressionCodecFactory codecFactory;

    public NovelToHdfsBolt() {
    }

    public NovelToHdfsBolt withCompressionCodec(String codec){
        this.compressionCodec = codec;
        return this;
    }

    public NovelToHdfsBolt withFsUrl(String fsUrl) {
        this.fsUrl = fsUrl;
        return this;
    }

    public NovelToHdfsBolt withConfigKey(String configKey){
        this.configKey = configKey;
        return this;
    }

    public NovelToHdfsBolt withFileNameFormat(FileNameFormat fileNameFormat) {
        this.fileNameFormat = fileNameFormat;
        return this;
    }

    public NovelToHdfsBolt withSequenceFormat(SequenceFormat format) {
        this.format = format;
        return this;
    }

    public NovelToHdfsBolt withSyncPolicy(SyncPolicy syncPolicy) {
        this.syncPolicy = syncPolicy;
        return this;
    }

    public NovelToHdfsBolt withRotationPolicy(FileRotationPolicy rotationPolicy) {
        this.rotationPolicy = rotationPolicy;
        return this;
    }

    public NovelToHdfsBolt withCompressionType(SequenceFile.CompressionType compressionType){
        this.compressionType = compressionType;
        return this;
    }

    public NovelToHdfsBolt addRotationAction(RotationAction action){
        this.rotationActions.add(action);
        return this;
    }

    @Override
    public void doPrepare(Map conf, TopologyContext topologyContext, OutputCollector collector) throws IOException {
        LOG.info("Preparing Sequence File Bolt...");
        if (this.format == null) throw new IllegalStateException("SequenceFormat must be specified.");

        this.fs = FileSystem.get(URI.create(this.fsUrl), hdfsConfig);
        this.codecFactory = new CompressionCodecFactory(hdfsConfig);
        initialJedisPool();
    }

    @Override
    public void execute(Tuple tuple) {
        try {
                long offset;
                synchronized (this.writeLock) {
                    this.writer.append(this.format.key(tuple), this.format.value(tuple));
                    offset = this.writer.getLength();

                    if (this.syncPolicy.mark(tuple, offset)) {
                        this.writer.hsync();
                        this.syncPolicy.reset();
                    }
                }
                this.collector.ack(tuple);
                if (this.rotationPolicy.mark(tuple, offset)) {
                    rotateOutputFile(); // synchronized
                    this.rotationPolicy.reset();
                }

        } catch (Exception e) {
            LOG.warn("write/sync failed.", e);
            this.collector.fail(tuple);
        }finally {
            //回收jedis实例
            if (jedis != null){
                jedis.close();
            }
        }

    }

    public Path createOutputFile() throws IOException {
        Path p = new Path(this.fsUrl + this.fileNameFormat.getPath(), this.fileNameFormat.getName(this.rotation, System.currentTimeMillis()));
        this.writer = SequenceFile.createWriter(
                this.hdfsConfig,
                SequenceFile.Writer.file(p),
                SequenceFile.Writer.keyClass(this.format.keyClass()),
                SequenceFile.Writer.valueClass(this.format.valueClass()),
                SequenceFile.Writer.compression(this.compressionType, this.codecFactory.getCodecByName(this.compressionCodec))
        );
        return p;
    }

    public void closeOutputFile() throws IOException {
        this.writer.close();
    }


    @Override
    public void declareOutputFields(OutputFieldsDeclarer outputFieldsDeclarer) {
       // this bolt
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
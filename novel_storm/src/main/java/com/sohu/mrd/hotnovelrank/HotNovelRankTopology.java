package com.sohu.mrd.hotnovelrank;

import com.sohu.mrd.hotnovelrank.bolt.kafka.NovelCountBolt;
import com.sohu.mrd.hotnovelrank.spout.kafka.KafkaSpout;
import org.apache.storm.Config;
import org.apache.storm.LocalCluster;
import org.apache.storm.StormSubmitter;
import org.apache.storm.generated.AlreadyAliveException;
import org.apache.storm.generated.AuthorizationException;
import org.apache.storm.generated.InvalidTopologyException;
import org.apache.storm.hdfs.bolt.HdfsBolt;
import org.apache.storm.hdfs.bolt.format.DefaultFileNameFormat;
import org.apache.storm.hdfs.bolt.format.DelimitedRecordFormat;
import org.apache.storm.hdfs.bolt.format.FileNameFormat;
import org.apache.storm.hdfs.bolt.format.RecordFormat;
import org.apache.storm.hdfs.bolt.rotation.FileRotationPolicy;
import org.apache.storm.hdfs.bolt.rotation.TimedRotationPolicy;
import org.apache.storm.hdfs.bolt.sync.CountSyncPolicy;
import org.apache.storm.hdfs.bolt.sync.SyncPolicy;
import org.apache.storm.hdfs.common.rotation.MoveFileAction;
import org.apache.storm.topology.TopologyBuilder;
import org.apache.storm.utils.Utils;

import java.io.IOException;
import java.text.SimpleDateFormat;

/**
 * Created by yonghongli on 2017/3/11.
 */
public class HotNovelRankTopology {
    static final String NOVEL_SPOUT_ID = "novel-spout";
    static final String BOLT_ID = "novel-bolt";
    static final String BOLT_ID_HDFS = "novel-hdfs-bolt";
    static final String TOPOLOGY_NAME = "hot-novel-topology";
    public static void main(String[] args) throws IOException {
        Config config = new Config();

        TopologyBuilder builder = new TopologyBuilder();

        NovelCountBolt bolt =new NovelCountBolt();
        builder.setSpout(NOVEL_SPOUT_ID, new KafkaSpout());
        builder.setBolt(BOLT_ID,bolt,5).shuffleGrouping(NOVEL_SPOUT_ID);

        if(args != null && args.length > 0){
            config.setNumWorkers(2);
            try {
                StormSubmitter.submitTopology(args[0], config, builder.createTopology());
            } catch (AlreadyAliveException e) {
                e.printStackTrace();
            } catch (InvalidTopologyException e) {
                e.printStackTrace();
            } catch (AuthorizationException e) {
                e.printStackTrace();
            }
        }else{
            LocalCluster cluster = new LocalCluster();
            cluster.submitTopology(TOPOLOGY_NAME,config,builder.createTopology());
            Utils.sleep(60000);
            cluster.killTopology(HotNovelRankTopology.class.getSimpleName());
            cluster.shutdown();
        }
    }
}

package com.sohu.mrd.formateuserbehavior;

import com.sohu.mrd.formateuserbehavior.bolt.FormateNovelLogBolt;
import com.sohu.mrd.formateuserbehavior.spout.NovelSourceSpout;
import org.apache.storm.Config;
import org.apache.storm.StormSubmitter;
import org.apache.storm.topology.TopologyBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * http://10.10.21.71:18080/index.html
 storm jar novel_storm-1.0-SNAPSHOT-shaded.jar  com.sohu.mrd.formateuserbehavior.FormateUserBehaviorTopology  novelFormateUserBehaviorTopology
   storm kill novelFormateUserBehaviorTopology
 * Created by yonghongli on 2017/3/11.
 */
public class FormateUserBehaviorTopology {
	private static final Logger LOG = LoggerFactory.getLogger(FormateUserBehaviorTopology.class);
	public static void main(String[] args) {
		try {
			 TopologyBuilder builder=new TopologyBuilder();
			 builder.setSpout("novelsourceSpout", new NovelSourceSpout(),3);
			 builder.setBolt("formateNovelLogBolt", new FormateNovelLogBolt(),3).shuffleGrouping("novelsourceSpout");
			 Config config = new Config();
			 config.setNumWorkers(3);
//			 LocalCluster cluster = new LocalCluster(); // storm本地调试
//			 cluster.submitTopology("test_test123", config, builder.createTopology());
			 StormSubmitter.submitTopology(args[0], config, builder.createTopology());
		} catch (Exception e) {
			LOG.error("启动异常  ",e);
		}
		
	}
}

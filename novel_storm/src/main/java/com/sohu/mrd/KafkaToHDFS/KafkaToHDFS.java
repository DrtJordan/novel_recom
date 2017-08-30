package com.sohu.mrd.KafkaToHDFS;

import com.sohu.mrd.util.KafkaKit;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.storm.hdfs.common.security.HdfsSecurityUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created by yonghongli on 2017/5/8.
 */
public class KafkaToHDFS {
    private static final Logger logger = LoggerFactory.getLogger(KafkaToHDFS.class);
    static LinkedBlockingQueue<String>   quene = new LinkedBlockingQueue<String>();
    static {
        new Thread(){
            @Override
            public void run() {
                KafkaKit.consumerMessage("behavior_novel_to_hdfs", "com_sohu_mrd_user_behavior_novel_2", 3, quene);

            }
        }.start();

    }
    public static void main(String[] args)  {
        try {
            HdfsSecurityUtil.login(new Configuration());
        } catch (IOException e) {
            e.printStackTrace();
        }
        FSDataOutputStream fo=null;

            while (true) {


                while (!quene.isEmpty()) {
                    try {
                        String msg = quene.poll();
                        String time = msg.split("\t", -1)[0].split(" ", -1)[0];
                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                        SimpleDateFormat sdf2 = new SimpleDateFormat("yyyyMMdd");
                        String date = sdf2.format(sdf.parse(time));

                        FileSystem fs = FileSystem.get(new Configuration());
                        String hdfs = "hdfs://heracles/user/mrd/novel_user_behivor/" + date;
                        if (!fs.exists(new Path(hdfs))) {
                            fs.mkdirs(new Path(hdfs));
                        }

                        if (!fs.exists(new Path(hdfs + "/user_behiver_data"))) {
                            fo = fs.create(new Path(hdfs + "/user_behiver_data"));

                        } else {
                            fo = fs.append(new Path(hdfs + "/user_behiver_data"));
                        }

                        fo.write((msg + "\n").getBytes("UTF-8"));
                        logger.info("msg:" + msg);
                        fo.close();
                    }catch (Exception e){

                    }
                }

            }



    }
}

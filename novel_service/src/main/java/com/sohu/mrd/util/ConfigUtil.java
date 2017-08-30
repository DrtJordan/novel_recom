package com.sohu.mrd.util;

import org.apache.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.Random;

/**
 * Created by yonghongli on 2017/3/10.
 */
public class ConfigUtil {
    private static final Logger logger = Logger.getLogger(ConfigUtil.class) ;

    public static Properties configProperties = getConfigProperties() ;

    static {
        Thread t = new Thread(){
            public void run(){
                while(true){
                    try {
                        configProperties = getConfigProperties() ;
                        sleep(5000l);
                        Random rand = new Random() ;
                        if(rand.nextInt(10) == 1) {
                            logger.info("configProperties:" + configProperties);
                        }
                    } catch (InterruptedException e) {
                        logger.error("",e);
                    }
                }
            }
        };
        t.setDaemon(true);
        t.start();
    }

    private static Properties getConfig(String filePath) {
        InputStream inputStream = null;
        Properties prop = new Properties();
        try {
            inputStream = ConfigUtil.class.getResourceAsStream(filePath);
            prop.load(inputStream);
        } catch (Exception e) {
            logger.error("init properties error: " + filePath, e);
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    logger.error("can't close the input stream!", e);
                }
            }
        }
        return prop;
    }

    private static Properties getConfigProperties(){
        return ConfigUtil.getConfig("/config/config.properties");
    }


}

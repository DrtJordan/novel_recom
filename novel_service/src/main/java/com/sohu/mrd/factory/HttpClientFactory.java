package com.sohu.mrd.factory;


import org.apache.http.client.config.RequestConfig;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.TimeUnit;

/**
 * Created by yonghongli on 2017/3/10.
 */
public class HttpClientFactory {
    private static PoolingHttpClientConnectionManager clientConnectionManager = null;

    private static final int MAX_TOTAL = 2000;                  //连接数

    private static final int DEFAULT_MAX_PER_ROUTE = 20;       //路由数

    private static RequestConfig requestConfig = null;


    static {
        clientConnectionManager = new PoolingHttpClientConnectionManager();
        clientConnectionManager.setMaxTotal(MAX_TOTAL);
        clientConnectionManager.setDefaultMaxPerRoute(DEFAULT_MAX_PER_ROUTE);

        requestConfig = RequestConfig.custom().setConnectTimeout(1000)      //创建连接时间
                .setConnectionRequestTimeout(1000)                          //从连接管理取得连接时间
                .setSocketTimeout(1000)                                    //数据传输时间
                .build();

        IdleConnectionMonitorThread monitorThread = new IdleConnectionMonitorThread(clientConnectionManager);
        monitorThread.start();
    }

    /**
     * 静态内部类，创建httpclient对象
     */
    static class CreateHttpClient {
        private static CloseableHttpClient httpClient = HttpClients.custom()
                .setDefaultRequestConfig(requestConfig)
                .setConnectionManager(clientConnectionManager)
                .build();
    }

    /**
     * 获取http client, 使用后不要使用close关闭连接池
     *
     * @return closeable httpClient
     */
    public static CloseableHttpClient getHttpClient() {
        return CreateHttpClient.httpClient;
    }


    private static class IdleConnectionMonitorThread extends Thread {

        private final HttpClientConnectionManager cm;
        private volatile boolean shutdown;
        private static final Logger log = LogManager.getLogger(IdleConnectionMonitorThread.class);

        public IdleConnectionMonitorThread(HttpClientConnectionManager cm) {
            super();
            this.cm = cm;
        }

        @Override
        public void run() {
            try {
                while (!shutdown) {
                    synchronized (this) {
                        wait(5000);
                        cm.closeExpiredConnections();
                        cm.closeIdleConnections(10, TimeUnit.SECONDS);
                        log.info("HttpClientConnectionManager close connections...");
                    }
                }
            } catch (InterruptedException e) {
                log.error(e.getMessage(),e);
            }
        }

        public void shutdown() {
            shutdown = true;
            synchronized (this) {
                notifyAll();
            }
        }

    }

}

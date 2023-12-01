package com.my.elasticsearch.config;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import com.my.elasticsearch.impl.MyEsServiceImpl;
import com.my.elasticsearch.MyEsService;
import com.my.elasticsearch.util.EsLog;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.annotation.PropertySources;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;

/**
 * es bean注册
 *
 * @Author nantian
 * @Date 2022/9/28 08:50
 * @Description:
 */
@Configuration
//设置es默认配置
@PropertySources({
    @PropertySource(value = "classpath:application-es-default.properties",ignoreResourceNotFound = true)
})
public class MyEsConfiguration implements InitializingBean {

    @Autowired
    private ElasticsearchRestTemplate elasticsearchRestTemplate;

    @Autowired
    private RestHighLevelClient restHighLevelClient;

    @Value("${my.elasticsearch.enableHeartbeat:true}")
    private boolean enableHeartbeat = true;


    @Bean
    public MyEsService cbEsService(){
        return new MyEsServiceImpl(elasticsearchRestTemplate);
    }

    /**
     * 定时任务发送es心跳，避免无请求后过端时间首次操作es出现Connection reset by peer问题
     */
    public void heartbeatToES(){
        try {
            ScheduledExecutorService scheduledExecutor = Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {
                @Override
                public Thread newThread(Runnable r) {
                    Thread thread = new Thread(r,"elasticsearch-heartBeat-thread");
                    return thread;
                }
            });
            RequestOptions requestOptions = RequestOptions.DEFAULT.toBuilder().build();
            Runnable runnable= new Runnable() {
                @Override
                public void run() {
                    try {
                        boolean result = restHighLevelClient.ping(requestOptions);
                        EsLog.debug("Send heartbeat to elasticsearch result:" + result);
                    } catch (Exception e) {
                        EsLog.warn("Send heartbeat to elasticsearch exception",e);
                    }
                }
            };
            scheduledExecutor.scheduleWithFixedDelay(runnable,60,60, TimeUnit.SECONDS);
        }catch (Throwable e){
            EsLog.log("heartbeatToES init exception",e);
        }
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        if (enableHeartbeat) {
            heartbeatToES();
        }
    }


    //@Value("${spring.elasticsearch.uris}")
    //private String esUris;
    ////es 原生api，暂不使用
    ////@Bean
    //public ElasticsearchClient elasticsearchClient() {
    //    RestClient restClient = RestClient.builder(
    //        new HttpHost("localhost", 9200)).build();
    //
    //    // Create the transport with a Jackson mapper
    //    ElasticsearchTransport transport = new RestClientTransport(
    //        restClient, new JacksonJsonpMapper());
    //
    //    // And create the API client
    //    ElasticsearchClient esClient = new ElasticsearchClient(transport);
    //    return esClient;
    //}
}

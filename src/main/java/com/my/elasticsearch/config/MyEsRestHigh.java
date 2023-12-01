package com.my.elasticsearch.config;

import java.net.URI;

import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.boot.autoconfigure.elasticsearch.ElasticsearchProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.PriorityOrdered;
import org.springframework.util.StringUtils;

/**
 * es bean注册
 *
 * @Author nantian
 * @Date 2022/9/28 08:50
 * @Description:
 */
@Configuration
//设置es默认配置
public class MyEsRestHigh implements PriorityOrdered {

    /**
     * 解决Connection reset by peer问题
     * @param builder
     * @param elasticsearchProperties
     * @return
     */
    @Bean
    public RestHighLevelClient restHighLevelClient(RestClientBuilder builder, ElasticsearchProperties elasticsearchProperties) {
        builder.setHttpClientConfigCallback(httpClientBuilder -> {
            // 配置用户身份凭据提供
            httpClientBuilder.setDefaultCredentialsProvider(
                new PropertiesCredentialsProvider(elasticsearchProperties));
            // 2分钟 设置超时时间，避免超长时间未操作ES出现连接reset问题
            httpClientBuilder.setKeepAliveStrategy((response, context) -> 1000 * 60);
            return httpClientBuilder;
        });
        return new RestHighLevelClient(builder);
    }

    @Override
    public int getOrder() {
        return -100000;
    }

    /**
     * 凭证提供器
     */
    private static class PropertiesCredentialsProvider extends BasicCredentialsProvider {
        PropertiesCredentialsProvider(ElasticsearchProperties properties) {
            if (StringUtils.hasText(properties.getUsername())) {
                Credentials credentials = new UsernamePasswordCredentials(properties.getUsername(), properties.getPassword());
                this.setCredentials(AuthScope.ANY, credentials);
            }

            properties.getUris().stream().map(this::toUri).filter(this::hasUserInfo).forEach(this::addUserInfoCredentials);
        }

        private URI toUri(String uri) {
            try {
                return URI.create(uri);
            } catch (IllegalArgumentException var3) {
                return null;
            }
        }

        private boolean hasUserInfo(URI uri) {
            return uri != null && StringUtils.hasLength(uri.getUserInfo());
        }

        private void addUserInfoCredentials(URI uri) {
            AuthScope authScope = new AuthScope(uri.getHost(), uri.getPort());
            Credentials credentials = this.createUserInfoCredentials(uri.getUserInfo());
            this.setCredentials(authScope, credentials);
        }

        private Credentials createUserInfoCredentials(String userInfo) {
            int delimiter = userInfo.indexOf(":");
            if (delimiter == -1) {
                return new UsernamePasswordCredentials(userInfo, (String)null);
            } else {
                String username = userInfo.substring(0, delimiter);
                String password = userInfo.substring(delimiter + 1);
                return new UsernamePasswordCredentials(username, password);
            }
        }
    }

}

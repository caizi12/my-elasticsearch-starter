# my-elasticsearch-starter
封装了易于使用的elasticsearch starter，使用时可以先把代码Deploy到私有仓库中，然后应用程序中依赖使用，如果没有私有仓库可以把代码copy到应用中使用。


Deploy到仓库后使用方式

###1、应用添加依赖

```
<dependency>
  <groupId>com.my.es</groupId>
  <artifactId>elasticsearch-starter</artifactId>
  <version>1.0.0-SNAPSHOT</version>     
</dependency>
```

### 2、application.properties 添加es链接配置

```
#es链接地址
spring.elasticsearch.uris=http://localhost:9200

#es账号密码，根据实际填写
spring.elasticsearch.username=elastic
spring.elasticsearch.password=123456
#可省配置：连接es集群超时参数，默认毫秒
spring.elasticsearch.connection-timeout=300
spring.elasticsearch.read-timeout=300
```

###3、Demo

# my-elasticsearch-starter
封装了易于使用的elasticsearch starter，使用时可以先把代码Deploy到私有仓库中，然后应用程序中依赖使用，如果没有私有仓库可以把代码copy到应用中使用。


Deploy到仓库后使用方式

### 1、应用添加依赖

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

### 3、Demo，更多示例可以看单元测试部分

```


@SpringBootTest
public class MyEsServiceTest {
    @Autowired
    private MyEsService myEsService;

    @Test
    public void delIndex() {
        boolean result = myEsService.deleteIndexIfExist(Student.class);
        Assert.assertTrue(result);
    }

    @Test
    public void delIndexDoc() {
        String result = myEsService.delIndexDoc("3007", Student.class);
        System.out.println("delIndexDoc:" + Student.class.getName());
    }


    @Test
    public void updateMapping() {
        boolean result = myEsService.updateIndexMapping(Student.class);
        Assert.assertTrue(result);
    }


    @Test
    public void updateIndexMapping() {
        boolean result = myEsService.updateIndexMapping(Shop.class);
        Assert.assertTrue(result);
    }

    @Test
    public void createIndex() {
        boolean exist = myEsService.existIndex(Student.class);
        boolean result = false;
        if (!exist) {
            result = myEsService.createIndexIfNotExist(Student.class);
        } else {
            System.out.println("index exist:" + Student.class.getName());
        }
        Assert.assertTrue(result);
    }


    @Test
    public void addIndexDoc() {
        Student student = new Student(1000, "张三", "测试索引添加", "哈哈", "三年二班刘", 10, new Date(), null);
        String documentId = myEsService.addIndexDoc(student);
        System.out.println("addIndexDoc result:" + documentId);
        Assert.assertNotNull(documentId);
    }
}



```

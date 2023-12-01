package com.my.es.test;

import java.util.Date;
import java.util.List;


import com.my.elasticsearch.MyEsService;
import com.my.es.test.model.Shop;
import com.my.es.test.model.Student;
import com.my.elasticsearch.model.MyEsSearchRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import net.minidev.json.JSONObject;
import org.assertj.core.util.Lists;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.MatchQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.RangeQueryBuilder;
import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.data.elasticsearch.core.IndexedObjectInformation;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.UpdateResponse;
import org.springframework.data.elasticsearch.core.query.UpdateResponse.Result;

/**
 * es demo
 *
 * @authro nantian
 * @date 2022-10-08 19:33
 */
@SpringBootTest
public class MyEsServiceTest {
    @Autowired
    private MyEsService myEsService;

    @Autowired
    private ElasticsearchRestTemplate elasticsearchRestTemplate;

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
    public void createIndex3() {
        boolean result = myEsService.createIndexIfNotExist(Shop.class);
        System.out.println("index exist:" + Shop.class.getName());
        Assert.assertTrue(result);
    }

    @Test
    public void addIndexDoc() {
        Student student = new Student(1000, "张三", "测试索引添加", "哈哈", "三年二班刘", 10, new Date(), null);
        String documentId = myEsService.addIndexDoc(student);
        System.out.println("addIndexDoc result:" + documentId);
        Assert.assertNotNull(documentId);
    }

    @Test
    public void saveIndexDocWithVersion() {
        Student student = new Student(1009, "张三1001", "测试索引添加1", "哈哈", "三年二班刘11", 10, new Date(), null);
        Student existOne = myEsService.findById(student.getId() + "", Student.class);
        Long _version = existOne != null ? existOne.getVersion() + 1 : null;
        String documentId = myEsService.saveIndexDoc(student, _version);
        System.out.println("addIndexDoc result:" + documentId);
        Assert.assertNotNull(documentId);
    }

    @Test
    public void existIndex() {
        boolean result1 = myEsService.existIndex(Student.class);
        boolean result2 = myEsService.existIndex(Student.class, true);
        System.out.println(result1 + "------" + result2);
    }

    @Test
    public void saveIndexDocWithNonTenantModel() {
        Student student = new Student(1001, "张三1001", "测试索引添加1", "哈哈", "三年二班刘11", 10, new Date(), null);
        boolean nonTenantModel = true;
        if (nonTenantModel) {
            if (!myEsService.existIndex(Student.class, nonTenantModel)) {
                myEsService.createIndexIfNotExist(Student.class, nonTenantModel);
            }

        }
        Student existOne = myEsService.findById(student.getId() + "", Student.class, nonTenantModel);
        Long _version = existOne != null ? existOne.getVersion() + 1 : null;
        String documentId = myEsService.saveIndexDoc(student, _version, nonTenantModel);
        System.out.println("addIndexDoc result:" + documentId);
        Assert.assertNotNull(documentId);
    }


    @Test
    public void bulkAddIndexDoc2() {
        Student student1 = new Student(1000, "zs0", "测试索引添加0", "哈哈33ss", "三年二班刘先生中国", 10, new Date(), null);
        Student student2 = new Student(1001, "zs", "测试索引添加1", "哈哈dd", "五年二班周先生美国", 20, new Date(), null);
        Student student3 = new Student(1002, "zs", "测试索引添加2", "哈哈aa", "10年二班刘女士中国", 0, new Date(), null);
        Student student4 = new Student(1003, "zs1003", "测试索引添加3", "哈哈aadd", "八年二班张女士北京", 50, new Date(), null);
        Student student5 = new Student(1004, "zs1004", "测试索引添加4", "哈哈bbaa", "三年二班刘重生北京", 60, new Date(), null);
        Student student6 = new Student(1006, "zs1006", "测试索引添加4", "哈哈bbaa", "三年二班刘重生北京", 60, new Date(), null);
        Student student7 = new Student(1007, "zs1007", "测试索引添加4", "哈哈bbaa", "三年二班刘重生北京", 60, new Date(), null);
        List list = Lists.newArrayList(student1, student2, student3, student4, student5, student6, student7);
        List<IndexedObjectInformation> result = myEsService.bulkAddIndexDoc(Student.class, list);
        System.out.println("bulkAddIndexDoc result:" + JSONObject.toJSONString(result));
        Assert.assertNotNull(result.size() > 0);
    }

    @Test
    public void bulkSaveIndexDoc() {
        Student student1 = new Student(1020, "zs0", "测试索引添加0", "哈哈33ss", "三年二班刘先生中国", 11, new Date(), null);
        Student student2 = new Student(1021, "zs", "测试索引添加1", "哈哈dd", "五年二班周先生美国", 12, new Date(), null);
        Student student3 = new Student(1022, "zs", "测试索引添加2", "哈哈aa", "10年二班刘女士中国", 13, new Date(), null);
        List<Student> list = Lists.newArrayList(student1, student2, student3);

        for (Student student : list) {
            Student existOne = myEsService.findById(student.getId() + "", Student.class);
            Long _version = existOne != null ? existOne.getVersion() + 1 : 1;
            student.setVersion(_version);
        }

        List<IndexedObjectInformation> result = myEsService.bulkSaveIndexDoc(Student.class, list);
        System.out.println("bulkAddIndexDoc result:" + JSONObject.toJSONString(result));
        Assert.assertNotNull(result.size() > 0);
    }

    @Test
    public void getByIdStudent() {
        Student student = myEsService.findById("1000", Student.class);
        System.out.println(JSONObject.toJSONString(student));
    }

    @Test
    public void updateDoc() throws JsonProcessingException {
        Student student = new Student();
        student.setId(1000);
        student.setAge(30);
        student.setText("lisi");
        UpdateResponse.Result result = myEsService.updateDoc(student);
        System.out.println("update result:" + JSONObject.toJSONString(result));
        Student student2 = myEsService.findById("1000", Student.class);
        System.out.println(JSONObject.toJSONString(student2));
        Assert.assertTrue(Result.UPDATED == result);
    }

    @Test
    public void searchAll() {
        SearchHits<Student> hits = myEsService.search(Student.class, QueryBuilders.matchAllQuery(), null);
        System.out.println(JSONObject.toJSONString(hits));
    }

    @Test
    public void searchBySingleField() {
        QueryBuilder queryBuilder = QueryBuilders.matchQuery("name", "zs0");
        SearchHits<Student> hits = myEsService.search(Student.class, queryBuilder, null);
        System.out.println(JSONObject.toJSONString(hits));
    }

    @Test
    public void searchByFilter() {
        MyEsSearchRequest request = new MyEsSearchRequest();
        request.setQueryFields(new String[]{"name", "id", "_version"});

        //1
        QueryBuilder queryBuilder = QueryBuilders.multiMatchQuery("zs", "name", "text");
        request.setQueryBuilder(queryBuilder);

        //2
        MatchQueryBuilder queryBuilder1 = QueryBuilders.matchQuery("name", "zs");
        RangeQueryBuilder queryBuilder2 = QueryBuilders.rangeQuery("age").gte("10").lte("60");
        MatchQueryBuilder fuzzyQueryBuilder = QueryBuilders.matchQuery("desc", "哈哈");

        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
        boolQueryBuilder.should(queryBuilder1);
        boolQueryBuilder.should(queryBuilder2);
        boolQueryBuilder.should(fuzzyQueryBuilder);

        BoolQueryBuilder filterQueryBuilder = QueryBuilders.boolQuery();
        filterQueryBuilder.should(QueryBuilders.matchQuery("id", "1000"));
        request.setFilterBuilder(filterQueryBuilder);

        //3 分页及排序
        request.setQueryBuilder(boolQueryBuilder);
        Sort sort = Sort.by(Direction.DESC, "age");
        PageRequest pageRequest = PageRequest.of(0, 10, sort);
        request.setPageable(pageRequest);

        SearchHits<Student> hits = myEsService.search(Student.class, request);
        System.out.println(JSONObject.toJSONString(hits));
    }
}

package com.my.es.test.util;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.Map;

import com.my.es.test.model.Student;
import org.junit.Test;
import org.springframework.data.elasticsearch.annotations.Document;

/**
 * @authro nantian
 * @date 2022-10-09 11:34
 */
public class EsUtilTest {
    @Test
    public void testAnanti() throws NoSuchFieldException, IllegalAccessException {
        Student s = new Student();
        org.springframework.data.elasticsearch.annotations.Document[] annotation = s.getClass().
            getAnnotationsByType(org.springframework.data.elasticsearch.annotations.Document.class);
        System.out.println(annotation.length);
        Document document = annotation[0];
        document = s.getClass().getAnnotation(Document.class);
        InvocationHandler h = Proxy.getInvocationHandler(document);
        Field hField = h.getClass().getDeclaredField("memberValues");
        hField.setAccessible(true);
        Map memberValues = (Map)hField.get(h);
        // 修改 value 属性值
        memberValues.put("indexName", "test" + String.valueOf(memberValues.get("indexName")));
        // 获取 foo 的 value 属性值
        System.out.println(memberValues.get("indexName"));
    }
}

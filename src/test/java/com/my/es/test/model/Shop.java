package com.my.es.test.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.Setting;

/**
 * @authro nantian
 * @date 2022-09-28 15:34
 */
@Data
@Document(indexName = "app_shop")
@Setting(shards = 3,replicas = 2)
@AllArgsConstructor
public class Shop {
    @Id
    private long id;

    @Field(type = FieldType.Keyword)
    private String name;

    @Field(type = FieldType.Keyword)
    private String text;

    @Field(type = FieldType.Integer)
    private int age;
}

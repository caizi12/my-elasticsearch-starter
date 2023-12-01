package com.my.es.test.model;

import java.util.Date;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

/**
 * @authro nantian
 * @date 2022-09-28 15:34
 */
@Data
@AllArgsConstructor
@Document(indexName = "app_student")
//@Setting(shards = 5,replicas = 1)
@NoArgsConstructor
public class Student {
    @Id
    private long id;

    @Field(type = FieldType.Keyword)
    private String name;

    @Field(type = FieldType.Keyword)
    private String text;

    //@Field(type = FieldType.Text,analyzer = "ik_max_word",searchAnalyzer = "ik_smart")
    @Field(type = FieldType.Keyword)
    private String desc;

    //@Field(type = FieldType.Text,analyzer = "ik_smart")
    @Field(type = FieldType.Keyword)
    private String title;

    @Field(type = FieldType.Integer)
    private Integer age;

    @Field(type = FieldType.Date)
    private Date date;

    @Version
    private Long version;
}

package com.my.elasticsearch.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.search.aggregations.AbstractAggregationBuilder;
import org.springframework.data.domain.Pageable;
import org.springframework.lang.Nullable;

/**
 *
 * es查询request,用于简化方法中过多参数传递；
 *
 * @authro nantian
 * @date 2022-10-12 10:09
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
public class MyEsSearchRequest {
    /**
     * 传入该属性，es会计算文档相关性分值，并按分值排序
     */
    private QueryBuilder queryBuilder;

    /**
     *
     * 过滤查询，精确查询场景适用性能更好
     *
     */
    private QueryBuilder filterBuilder;
    /**
     * 分页和排序
     */
    private Pageable pageable;
    /**
     * 用于指定查询结果返回哪些字段
     */
    private String[] queryFields;
    /**
     * 聚合查询
     */
    @Nullable
    private AbstractAggregationBuilder aggregationBuilder;

    public MyEsSearchRequest(QueryBuilder queryBuilder, QueryBuilder filterBuilder,
                             Pageable pageable) {
        this.queryBuilder = queryBuilder;
        this.filterBuilder = filterBuilder;
        this.pageable = pageable;
    }

    public MyEsSearchRequest(QueryBuilder filterBuilder, @Nullable Pageable pageable, String[] queryFields) {
        this.filterBuilder = filterBuilder;
        this.pageable = pageable;
        this.queryFields = queryFields;
    }

    public MyEsSearchRequest(QueryBuilder queryBuilder, QueryBuilder filterBuilder,
                             @Nullable Pageable pageable, String[] queryFields) {
        this.queryBuilder = queryBuilder;
        this.filterBuilder = filterBuilder;
        this.pageable = pageable;
        this.queryFields = queryFields;
    }
}

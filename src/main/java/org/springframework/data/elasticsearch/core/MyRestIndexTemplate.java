package org.springframework.data.elasticsearch.core;

import java.util.Map;

import org.springframework.data.elasticsearch.core.document.Document;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.lang.Nullable;

/**
 *
 * 重写doPutMapping，修改为public
 *
 * @authro LiuLiLiang
 * @date 2022-10-11 17:15
 */
public class MyRestIndexTemplate extends RestIndexTemplate {

    public MyRestIndexTemplate(ElasticsearchRestTemplate restTemplate, Class<?> boundClass) {
        super(restTemplate, boundClass);
    }

    public MyRestIndexTemplate(ElasticsearchRestTemplate restTemplate,
                               IndexCoordinates boundIndex) {
        super(restTemplate, boundIndex);
    }

    @Override
    public boolean doPutMapping(IndexCoordinates index, Document mapping){
        return super.doPutMapping(index,mapping);
    }

    /**
     * 覆盖doCreate
     *
     *   开放可以支持自定义传入setting和mapping进行创建索引的能力
     *
     * @param index
     * @param settings
     * @param mapping
     * @return
     */
    @Override
    public boolean doCreate(IndexCoordinates index, Map<String, Object> settings, @Nullable Document mapping){
        return super.doCreate(index,settings,mapping);
    }
}

package com.my.elasticsearch.cache;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.my.elasticsearch.util.EsReflectUtils;
import org.apache.commons.lang3.StringUtils;

/**
 * 从索引类上获取@Document注解索引名
 *
 * @authro nantian
 * @date 2022-10-10 09:30
 */
public class EsIndexNameCache {
    /**
     * 索引类与索引名缓存
     */
    private final static Map<Class<?>, String> DOCUMENT_INDEX_NAME_MAP = new ConcurrentHashMap();

    /**
     * 获取索引名
     *
     * @param clazz
     * @return
     */
    public static String get(Class<?> clazz) {
        if (clazz == null) {
            return null;
        }

        String indexName = DOCUMENT_INDEX_NAME_MAP.get(clazz);
        if (StringUtils.isNotBlank(indexName)) {
            return indexName;
        }

        indexName = EsReflectUtils.getDocumentIndexName(clazz);
        //暂不考虑并发问题，即使有并发对同一个class来说也是重写数据，不会出现数据错乱问题
        DOCUMENT_INDEX_NAME_MAP.put(clazz, indexName);
        return indexName;
    }
}

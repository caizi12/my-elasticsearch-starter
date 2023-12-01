package com.my.elasticsearch.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.my.elasticsearch.MyEsService;
import com.my.elasticsearch.cache.EsIndexNameCache;
import com.my.elasticsearch.util.EsReflectUtils;
import com.my.elasticsearch.util.EsTenantUtil;
import com.my.elasticsearch.model.MyEsSearchRequest;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.aggregations.AbstractAggregationBuilder;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.data.elasticsearch.core.IndexedObjectInformation;
import org.springframework.data.elasticsearch.core.MultiGetItem;
import org.springframework.data.elasticsearch.core.MyRestIndexTemplate;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.document.Document;
import org.springframework.data.elasticsearch.core.index.Settings;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.BulkOptions;
import org.springframework.data.elasticsearch.core.query.FetchSourceFilter;
import org.springframework.data.elasticsearch.core.query.IndexQuery;
import org.springframework.data.elasticsearch.core.query.IndexQueryBuilder;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.data.elasticsearch.core.query.StringQuery;
import org.springframework.data.elasticsearch.core.query.UpdateQuery;
import org.springframework.data.elasticsearch.core.query.UpdateResponse;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

/**
 * es 公共组件服务实现
 *
 *  该实现提供了租户也非租户模式，租户模式会给索引名前自动加租户code前缀，如果不需要可以进行修改
 *
 * @authro nantian
 * @date 2022-10-08 15:19
 */
public class MyEsServiceImpl implements MyEsService {
    private static ObjectMapper objectMapper;
    private ElasticsearchRestTemplate elasticsearchRestTemplate;
    private static final String PROPERTIES_KEY = "properties";

    public MyEsServiceImpl(ElasticsearchRestTemplate elasticsearchRestTemplate) {
        this.elasticsearchRestTemplate = elasticsearchRestTemplate;
    }

    static {
        //JavaTimeModule timeModule = new JavaTimeModule();
        //timeModule.addSerializer(LocalDateTime.class, new LocalDateTimeSerializer());
        //timeModule.addDeserializer(LocalDateTime.class, new LocalDateTimeDeserializer());
        //// 设置NULL值不参与序列化
        objectMapper = new ObjectMapper().setSerializationInclusion(JsonInclude.Include.NON_NULL);
        //.registerModule(timeModule);
    }

    /**
     * 根据使用的租户模式，决定是否对索引名添加租户标识
     *
     * @param index
     * @return
     */
    private String convertTenantIndex(String index) {
        return EsTenantUtil.getTenantIndex(index);
    }

    /**
     * 构建操作es的索引类
     *
     * @param index
     * @param nonTenantMode 是否是租户模式，true表示非租户模式，即通用索引
     * @return
     */
    private IndexCoordinates buildIndexCoordinates(String index, boolean nonTenantMode) {
        if (!nonTenantMode) {
            index = convertTenantIndex(index);
        }
        return IndexCoordinates.of(index);
    }

    private IndexCoordinates buildIndexCoordinates(Class<?> clazz) {
        return buildIndexCoordinates(clazz, false);
    }

    private IndexCoordinates buildIndexCoordinates(Class<?> clazz, boolean nonTenantMode) {
        if (!nonTenantMode) {
            return IndexCoordinates.of(convertTenantIndex(getEsIndexName(clazz)));
        }
        return IndexCoordinates.of(getEsIndexName(clazz));
    }

    /**
     * 根据类@Document(indexName)属性获取索引名
     *
     * @param clazz
     * @return
     */
    private String getEsIndexName(Class<?> clazz) {
        return EsIndexNameCache.get(clazz);
    }

    /**
     * 判断索引是否存在
     *
     * @param indexName     索引名称
     * @param nonTenantMode 是否是租户模式，true表示非租户模式，即通用索引
     * @return
     */
    public boolean existIndex(String indexName, boolean nonTenantMode) {
        if (StringUtils.isNotEmpty(indexName)) {
            return elasticsearchRestTemplate.indexOps(buildIndexCoordinates(indexName, nonTenantMode)).exists();
        }
        return Boolean.FALSE;
    }

    /**
     * 判断索引是否存在
     *
     * @param clazz
     * @return
     */
    @Override
    public boolean existIndex(Class<?> clazz) {
        return existIndex(clazz, false);
    }

    @Override
    public boolean existIndex(Class<?> clazz, boolean nonTenantMode) {
        if (clazz != null) {
            return existIndex(getEsIndexName(clazz), nonTenantMode);
        }
        return Boolean.FALSE;
    }

    /**
     * 索引不存在时创建索引[无分片及mapping信息,暂不开放使用]
     *
     * @param indexName 索引名称
     * @return 是否创建成功
     */
    private boolean createIndexIfNotExist(String indexName) {
        return createIndexIfNotExist(indexName, false);
    }

    /**
     * 索引不存在时创建索引[无分片及mapping信息,暂不开放使用]
     *
     * @param indexName     索引名称
     * @param nonTenantMode 是否是租户模式，true表示非租户模式，即通用索引
     * @return 是否创建成功
     */
    private boolean createIndexIfNotExist(String indexName, boolean nonTenantMode) {
        if (!existIndex(indexName, nonTenantMode)) {
            return elasticsearchRestTemplate.indexOps(buildIndexCoordinates(indexName, nonTenantMode)).create();
        }
        return Boolean.FALSE;
    }

    /**
     * 索引不存在时创建索引并设置分片及mapping信息
     *
     * @param clazz 索引类信息
     * @return 是否创建成功
     */
    @Override
    public boolean createIndexIfNotExist(Class<?> clazz) {
        return createIndexIfNotExist(clazz, false);
    }

    @Override
    public boolean createIndexIfNotExist(Class<?> clazz, boolean nonTenantMode) {
        boolean result = existIndex(clazz, nonTenantMode);
        if (!result) {
            //不能直接用createWithMapping，会漏掉租户信息，改写索引类上租户实现比较复杂
            //elasticsearchRestTemplate.indexOps(clazz).createWithMapping();
            MyRestIndexTemplate esRestIndexTemplate = new MyRestIndexTemplate(elasticsearchRestTemplate, clazz);
            Document document = esRestIndexTemplate.createMapping();
            Settings settings = esRestIndexTemplate.createSettings();

            return esRestIndexTemplate.doCreate(buildIndexCoordinates(clazz, nonTenantMode), settings, document);
        }
        return Boolean.FALSE;
    }

    /**
     * 更新索引字段，会自动获取类上的索引注解信息进行更新索引mapping，已存在的字段不会更新
     *
     * @param clazz
     * @return
     */
    @Override
    public boolean updateIndexMapping(Class<?> clazz) {
        return updateIndexMapping(clazz, false);
    }

    @Override
    public boolean updateIndexMapping(Class<?> clazz, boolean nonTenantMode) {
        boolean result = existIndex(clazz, nonTenantMode);
        if (result) {
            MyRestIndexTemplate esRestIndexTemplate = new MyRestIndexTemplate(elasticsearchRestTemplate, clazz);
            Document document = esRestIndexTemplate.createMapping();
            return esRestIndexTemplate.doPutMapping(buildIndexCoordinates(clazz, nonTenantMode), document);
        }
        return Boolean.FALSE;
    }

    /**
     * 索引存在删除索引
     *
     * @param indexName 索引名称
     * @return 是否删除成功
     */
    public boolean deleteIndexIfExist(String indexName) {
        return deleteIndexIfExist(indexName, false);
    }

    /**
     * 索引存在删除索引
     *
     * @param indexName 索引名称
     * @return 是否删除成功
     */
    public boolean deleteIndexIfExist(String indexName, boolean nonTenantMode) {
        if (existIndex(indexName, nonTenantMode)) {
            return elasticsearchRestTemplate.indexOps(buildIndexCoordinates(indexName, nonTenantMode)).delete();
        }
        return Boolean.FALSE;
    }

    /**
     * 索引存在删除索引
     *
     * @param clazz 索引名称
     * @return 是否删除成功
     */
    @Override
    public boolean deleteIndexIfExist(Class<?> clazz) {
        if (existIndex(clazz)) {
            return deleteIndexIfExist(getEsIndexName(clazz));
        }
        return Boolean.FALSE;
    }

    @Override
    public boolean deleteIndexIfExist(Class<?> clazz, boolean nonTenantMode) {
        if (existIndex(clazz, nonTenantMode)) {
            return deleteIndexIfExist(getEsIndexName(clazz), nonTenantMode);
        }
        return Boolean.FALSE;
    }

    /**
     * 新增索引文档，根据类上的@Document获取索引名
     *
     * @param model elasticsearch文档; 文档需标注@Document注解、包含@Id注解字段, 且@Id注解标注的文档ID字段值不能为空
     * @return 文档ID
     */
    @Override
    public <T> String addIndexDoc(T model) {
        return addIndexDoc(getEsIndexName(model.getClass()), model);
    }

    @Override
    public <T> String addIndexDoc(T model, boolean nonTenantMode) {
        return addIndexDoc(getEsIndexName(model.getClass()), model, nonTenantMode);
    }

    /**
     * 新增文档,指定索引名
     *
     * @param indexName 索引名称
     * @param model     es文档; 文档需标注@Document注解、包含@Id注解字段, 且@Id注解标注的文档ID字段值不能为空
     * @return 文档ID
     */
    @Override
    public <T> String addIndexDoc(String indexName, T model) {
        return addIndexDoc(indexName, model, false);
    }

    @Override
    public <T> String addIndexDoc(String indexName, T model, boolean nonTenantMode) {
        Assert.notNull(indexName, "addIndexDoc elasticsearch indexName is null");
        Assert.notNull(model, "addIndexDoc document is null");
        return elasticsearchRestTemplate.index(
                new IndexQueryBuilder().withId(getDocumentIdValue(model)).withObject(model).build(),
                buildIndexCoordinates(indexName, nonTenantMode));
    }

    /**
     * 保存文档，指定数据版本号
     *
     * @param model   es文档; 文档需标注@Document注解、包含@Id注解字段, 且@Id注解标注的文档ID字段值不能为空
     * @param version 数据版本号
     * @param <T>
     * @return
     */
    @Override
    public <T> String saveIndexDoc(T model, Long version) {
        return saveIndexDoc(model, version, false);
    }

    @Override
    public <T> String saveIndexDoc(T model, Long version, boolean nonTenantMode) {
        return saveIndexDoc(getEsIndexName(model.getClass()), model, version, nonTenantMode);
    }

    /**
     * 保存文档，指定索引名和数据版本号
     *
     * @param indexName 索引名称
     * @param model     es文档; 文档需标注@Document注解、包含@Id注解字段, 且@Id注解标注的文档ID字段值不能为空
     * @param version   数据版本号
     * @return 文档ID
     */
    @Override
    public <T> String saveIndexDoc(String indexName, T model, Long version) {
        return saveIndexDoc(indexName, model, version, false);
    }

    /**
     * 保存文档，指定索引名和数据版本号
     *
     * @param indexName     索引名称
     * @param model         es文档; 文档需标注@Document注解、包含@Id注解字段, 且@Id注解标注的文档ID字段值不能为空
     * @param version       数据版本号
     * @param nonTenantMode 是否是租户模式，true表示非租户模式，即通用索引
     * @return 文档ID
     */
    @Override
    public <T> String saveIndexDoc(String indexName, T model, Long version, boolean nonTenantMode) {
        Assert.notNull(indexName, "addIndexDoc elasticsearch indexName is null");
        Assert.notNull(model, "addIndexDoc document is null");
        return elasticsearchRestTemplate.index(
                new IndexQueryBuilder().withId(getDocumentIdValue(model)).withVersion(version).withObject(model).build(),
                buildIndexCoordinates(indexName, nonTenantMode));
    }

    @Override
    public <T> List<IndexedObjectInformation> bulkAddIndexDoc(Class<?> clazz, List<T> docList) {
        return bulkAddIndexDoc(getEsIndexName(clazz), docList);
    }

    @Override
    public <T> List<IndexedObjectInformation> bulkAddIndexDoc(Class<?> clazz, List<T> docList, boolean nonTenantMode) {
        return bulkAddIndexDoc(getEsIndexName(clazz), docList, nonTenantMode);
    }

    @Override
    public <T> List<IndexedObjectInformation> bulkSaveIndexDoc(Class<?> clazz, List<T> docList) {
        return bulkSaveIndexDoc(clazz, docList, false);
    }

    @Override
    public <T> List<IndexedObjectInformation> bulkSaveIndexDoc(Class<?> clazz, List<T> docList, boolean nonTenantMode) {
        return bulkSaveIndexDoc(getEsIndexName(clazz), docList, nonTenantMode);
    }

    /**
     * 批量新增文档
     *
     * @param indexName 索引名称
     * @param docList   es文档集合; 文档需标注@Document注解、包含@Id注解字段, 且@Id注解标注的文档ID字段值不能为空
     * @return 文档ID
     */
    public <T> List<IndexedObjectInformation> bulkAddIndexDoc(String indexName, List<T> docList) {
        return bulkAddIndexDoc(indexName, docList, false);
    }

    /**
     * 批量新增文档
     *
     * @param indexName     索引名称
     * @param docList       es文档集合; 文档需标注@Document注解、包含@Id注解字段, 且@Id注解标注的文档ID字段值不能为空
     * @param nonTenantMode 是否是租户模式，true表示非租户模式，即通用索引
     * @return 文档ID
     */
    @Override
    public <T> List<IndexedObjectInformation> bulkAddIndexDoc(String indexName, List<T> docList, boolean nonTenantMode) {
        Assert.notNull(indexName, "bulkAddIndexDoc elasticsearch indexName is null");
        Assert.notNull(docList, "bulkAddIndexDoc document is null");

        List<IndexQuery> indexQueries = new ArrayList<>();
        docList.forEach(doc ->
                indexQueries.add(new IndexQueryBuilder().withId(getDocumentIdValue(doc)).withObject(doc).build()));

        return elasticsearchRestTemplate.bulkIndex(indexQueries, buildIndexCoordinates(indexName, nonTenantMode));
    }

    /**
     * 批量新增文档
     *
     * @param indexName 索引名称
     * @param docList   es文档集合; 文档需标注@Document注解、包含@Id、@Version注解字段, 且@Id注解标注的文档ID字段值不能为空、@Version注解标注的文档数据版本字段值不能为空
     * @param <T>
     * @return
     */
    @Override
    public <T> List<IndexedObjectInformation> bulkSaveIndexDoc(String indexName, List<T> docList) {
        return bulkSaveIndexDoc(indexName, docList, false);
    }

    /**
     * 批量新增文档
     *
     * @param indexName     索引名称
     * @param docList       es文档集合; 文档需标注@Document注解、包含@Id、@Version注解字段, 且@Id注解标注的文档ID字段值不能为空、@Version注解标注的文档数据版本字段值不能为空
     * @param nonTenantMode 是否是租户模式，true表示非租户模式，即通用索引
     * @return
     */
    @Override
    public <T> List<IndexedObjectInformation> bulkSaveIndexDoc(String indexName, List<T> docList, boolean nonTenantMode) {
        Assert.notNull(indexName, "bulkAddIndexDoc elasticsearch indexName is null");
        Assert.notNull(docList, "bulkAddIndexDoc document is null");

        // 验证是否传version值
        docList.forEach(doc -> getDocumentVersionValue(doc));

        List<IndexQuery> indexQueries = new ArrayList<>();
        docList.forEach(doc ->
                indexQueries.add(new IndexQueryBuilder().withId(getDocumentIdValue(doc)).withVersion(getDocumentVersionValue(doc)).withObject(doc).build()));

        return elasticsearchRestTemplate.bulkIndex(indexQueries, buildIndexCoordinates(indexName, nonTenantMode));
    }

    /**
     * 根据ID查询文档
     *
     * @param indexName     索引名称
     * @param docId         文档ID
     * @param clazz         映射类Class
     * @param nonTenantMode 是否是租户模式，true表示非租户模式，即通用索引
     * @param <T>
     * @return Elasticsearch 文档
     */
    public <T> T findById(String indexName, String docId, Class<T> clazz, boolean nonTenantMode) {
        if (StringUtils.isNotEmpty(docId) && clazz != null) {
            return elasticsearchRestTemplate.get(docId, clazz, buildIndexCoordinates(indexName, nonTenantMode));
        }
        return null;
    }

    public <T> T findById(String docId, Class<T> clazz) {
        return findById(docId, clazz, false);
    }

    @Override
    public <T> T findById(String docId, Class<T> clazz, boolean nonTenantMode) {
        return findById(getEsIndexName(clazz), docId, clazz, nonTenantMode);
    }


    /**
     * 根据多个ID查询文档
     *
     * @param indexName     索引名称
     * @param docIdList      文档ID
     * @param clazz         映射类Class
     * @param nonTenantMode 是否是租户模式，true表示非租户模式，即通用索引
     * @param <T>
     * @return Elasticsearch 文档
     */
    @Override
    public <T> List<T> findByIds(String indexName, Class<T> clazz, List<String> docIdList, boolean nonTenantMode) {
        if (CollectionUtils.isEmpty(docIdList) || clazz == null || indexName == null) {
            return null;
        }
        StringQuery query = StringQuery.builder("stringQuery").withIds(docIdList).build();
        List<MultiGetItem<T>> result = elasticsearchRestTemplate.multiGet(query, clazz, buildIndexCoordinates(indexName, nonTenantMode));
        if(CollectionUtils.isEmpty(result)){
            return null;
        }

        List list = result.stream().map(o->o.getItem()).filter(item->item != null).collect(Collectors.toList());
        return list;
    }

    @Override
    public <T> List<T> findByIds(Class<T> clazz,List<String> docIdList) {
        return findByIds( clazz, docIdList,false);
    }

    @Override
    public <T> List<T> findByIds(Class<T> clazz, List<String> docIdList,boolean nonTenantMode) {
        return findByIds(getEsIndexName(clazz),  clazz,docIdList, nonTenantMode);
    }

    /**
     * 根据ID判断文档是否存在
     *
     * @param indexName 索引名称
     * @param docId     文档ID
     * @return 存在与否
     */
    private boolean existDocById(String indexName, String docId, boolean nonTenantMode) {
        if (existIndex(indexName, nonTenantMode) && StringUtils.isNotEmpty(docId)) {
            return elasticsearchRestTemplate.exists(docId, buildIndexCoordinates(indexName, nonTenantMode));
        }
        return Boolean.FALSE;
    }

    public boolean existDocById(Class<?> clazz, String docId) {
        return existDocById(clazz, docId, false);
    }

    @Override
    public boolean existDocById(Class<?> clazz, String docId, boolean nonTenantMode) {
        return existDocById(getEsIndexName(clazz), docId, nonTenantMode);
    }

    public <T> UpdateResponse.Result updateDoc(T elasticsearchModel) {
        return updateDoc(elasticsearchModel, false);
    }

    @Override
    public <T> UpdateResponse.Result updateDoc(T elasticsearchModel, boolean nonTenantMode) {
        String indexName = getEsIndexName(elasticsearchModel.getClass());
        return updateDoc(indexName, elasticsearchModel, nonTenantMode);
    }

    /**
     * 更新文档
     *
     * @param indexName          索引名称
     * @param elasticsearchModel elasticsearch文档; 文档需标注@Document注解、包含@Id注解字段, 且@Id标注的文档ID值不能为空
     * @return UpdateResponse.Result
     * @throws JsonProcessingException JsonProcessingException
     */
    private <T> UpdateResponse.Result updateDoc(String indexName, T elasticsearchModel, boolean nonTenantMode) {
        return updateDoc(indexName, elasticsearchModel, this.objectMapper, nonTenantMode);
    }

    /**
     * 更新文档
     *
     * @param indexName          索引名称
     * @param elasticsearchModel elasticsearch文档; 文档需标注@Document注解、包含@Id注解字段, 且@Id标注的文档ID值不能为空
     * @param objectMapper       objectMapper
     * @return UpdateResponse.Result
     */
    private <T> UpdateResponse.Result updateDoc(String indexName, T elasticsearchModel, ObjectMapper objectMapper) {
        return updateDoc(indexName, elasticsearchModel, objectMapper, false);
    }

    /**
     * 更新文档
     *
     * @param indexName          索引名称
     * @param elasticsearchModel elasticsearch文档; 文档需标注@Document注解、包含@Id注解字段, 且@Id标注的文档ID值不能为空
     * @param objectMapper       objectMapper
     * @param nonTenantMode      是否是租户模式，false表示非租户模式，即通用索引
     * @return UpdateResponse.Result
     */
    private <T> UpdateResponse.Result updateDoc(String indexName, T elasticsearchModel, ObjectMapper objectMapper, boolean nonTenantMode) {
        Assert.notNull(indexName, "bulkUpdateDoc clazz is null");
        Assert.notNull(elasticsearchModel, "bulkUpdateDoc modelList is null");
        try {
            String id = getDocumentIdValue(elasticsearchModel);
            Assert.isTrue(existDocById(indexName, id, nonTenantMode), "elasticsearch document is not exist.");

            objectMapper = objectMapper == null ? this.objectMapper : objectMapper;
            String json = objectMapper.writeValueAsString(elasticsearchModel);
            UpdateQuery updateQuery = UpdateQuery.builder(id).withDocument(Document.parse(json)).build();

            return elasticsearchRestTemplate.update(updateQuery, buildIndexCoordinates(indexName, nonTenantMode)).getResult();
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public <T> List<IndexedObjectInformation> bulkUpdateDoc(Class<?> clazz, List<T> modelList) {
        return bulkUpdateDoc(clazz, modelList, null);
    }

    public <T> List<IndexedObjectInformation> bulkUpdateDoc(Class<?> clazz, List<T> modelList, BulkOptions bulkOptions) {
        return bulkUpdateDoc(clazz, modelList, bulkOptions, objectMapper);
    }


    /**
     * 批量更新文档
     *
     * @param clazz        索引名称
     * @param modelList    elasticsearch文档; 文档需标注@Document注解、包含@Id注解字段, 且@Id标注的文档ID值不能为空
     * @param objectMapper objectMapper
     * @return UpdateResponse.Result
     */
    private <T> List<IndexedObjectInformation> bulkUpdateDoc(Class<?> clazz, List<T> modelList, BulkOptions bulkOptions,
                                                             ObjectMapper objectMapper) {
        Assert.notNull(clazz, "bulkUpdateDoc clazz is null");
        Assert.notNull(clazz, "bulkUpdateDoc modelList is null");

        try {
            List<UpdateQuery> queries = new ArrayList(modelList.size());
            UpdateQuery updateQuery = null;
            String id = null;
            for (T model : modelList) {
                id = getDocumentIdValue(model);
                Assert.notNull(id, clazz.getName() + " instance document id is null");
                String json = objectMapper.writeValueAsString(model);
                updateQuery = UpdateQuery.builder(getDocumentIdValue(model)).withDocument(Document.parse(json)).build();
                queries.add(updateQuery);
            }
            bulkOptions = bulkOptions == null ? BulkOptions.defaultOptions() : bulkOptions;
            return elasticsearchRestTemplate.doBulkOperation(queries, bulkOptions, buildIndexCoordinates(clazz));
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    private <T> String getDocumentIdValue(T elasticsearchModel) {
        return EsReflectUtils.getDocumentIdValue(elasticsearchModel);
    }

    private <T> Long getDocumentVersionValue(T elasticsearchModel) {
        return EsReflectUtils.getDocumentVersionValue(elasticsearchModel);
    }

    /**
     * 查询文档
     *
     * @param clazz         映射文档类 文档需标注@Document注解、包含@Id注解字段
     * @param queryBuilder  非结构化数据 QueryBuilder; queryBuilder与filterBuilder必须二者存在其一
     * @param filterBuilder 过滤查询
     * @param <T>
     * @return
     */
    public <T> SearchHits<T> search(Class<T> clazz, QueryBuilder queryBuilder, QueryBuilder filterBuilder,
                                    Pageable pageable) {
        MyEsSearchRequest request = new MyEsSearchRequest(queryBuilder, filterBuilder, pageable);
        return search(clazz, request);
    }

    @Override
    public <T> SearchHits<T> search(Class<T> clazz, QueryBuilder queryBuilder, QueryBuilder filterBuilder,
                                    Pageable pageable, boolean nonTenantMode) {
        MyEsSearchRequest request = new MyEsSearchRequest(queryBuilder, filterBuilder, pageable);
        return search(clazz, request, nonTenantMode);
    }

    public <T> SearchHits<T> searchByFilter(Class<T> clazz, QueryBuilder filterBuilder, Pageable pageable) {
        MyEsSearchRequest request = new MyEsSearchRequest(null, filterBuilder, pageable);
        return search(clazz, request);
    }

    @Override
    public <T> SearchHits<T> searchByFilter(Class<T> clazz, QueryBuilder filterBuilder, @javax.annotation.Nullable Pageable pageable, boolean nonTenantMode) {
        MyEsSearchRequest request = new MyEsSearchRequest(null, filterBuilder, pageable);
        return search(clazz, request, nonTenantMode);
    }

    public <T> SearchHits<T> search(Class<T> clazz, QueryBuilder queryBuilder, Pageable pageable) {
        MyEsSearchRequest request = new MyEsSearchRequest(queryBuilder, null, pageable);
        return search(clazz, request);
    }

    @Override
    public <T> SearchHits<T> search(Class<T> clazz, QueryBuilder queryBuilder, @javax.annotation.Nullable Pageable pageable, boolean nonTenantMode) {
        MyEsSearchRequest request = new MyEsSearchRequest(queryBuilder, null, pageable);
        return search(clazz, request, nonTenantMode);
    }

    public <T> SearchHits<T> search(Class<T> clazz, MyEsSearchRequest request) {
        return search(clazz, request, false);
    }

    @Override
    public <T> SearchHits<T> search(Class<T> clazz, MyEsSearchRequest request, boolean nonTenantMode) {
        return search(getEsIndexName(clazz), clazz, request.getQueryBuilder(), request.getFilterBuilder(),
                request.getAggregationBuilder(), request.getPageable(), request.getQueryFields(), nonTenantMode);
    }

    public <T> SearchHits<T> search(Class<T> clazz, NativeSearchQueryBuilder queryBuilder) {
        return search(clazz, queryBuilder, false);
    }

    @Override
    public <T> SearchHits<T> search(Class<T> clazz, NativeSearchQueryBuilder queryBuilder, boolean nonTenantMode) {
        return elasticsearchRestTemplate.search(queryBuilder.build(), clazz, buildIndexCoordinates(clazz, nonTenantMode));
    }

    /**
     * 查询文档
     *
     * <p>
     * 查询的文档必须包含映射@Document的@Id字段
     * </p>
     *
     * @param indexName                  索引名称
     * @param clazz                      映射文档类 文档需标注@Document注解、包含@Id注解字段
     * @param queryBuilder               非结构化数据 QueryBuilder; queryBuilder与filterBuilder必须二者存在其一
     * @param filterBuilder              结构化数据 QueryBuilder; filterBuilder与queryBuilder必须二者存在其一
     * @param abstractAggregationBuilder 聚合查询Builder
     * @param pageable                   分页/排序; 分页从0开始
     * @param fields                     包含字段
     * @param nonTenantMode              是否是租户模式，false表示非租户模式，即通用索引
     * @return
     */
    private <T> SearchHits<T> search(String indexName, Class<T> clazz, @Nullable QueryBuilder queryBuilder,
                                     @Nullable QueryBuilder filterBuilder,
                                     @Nullable AbstractAggregationBuilder abstractAggregationBuilder,
                                     @Nullable Pageable pageable, @Nullable String[] fields, boolean nonTenantMode) {
        if (StringUtils.isNotBlank(indexName)) {
            // 查询的文档必须包含映射@Document的@Id字段（
            BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery().must(
                    QueryBuilders.existsQuery(EsReflectUtils.getDocumentIdFieldName(clazz)));
            if (queryBuilder != null) {
                boolQueryBuilder.must(queryBuilder);
            }
            NativeSearchQueryBuilder nativeSearchQueryBuilder = new NativeSearchQueryBuilder().withQuery(
                    boolQueryBuilder);
            if (filterBuilder != null) {
                nativeSearchQueryBuilder.withFilter(filterBuilder);
            }
            if (abstractAggregationBuilder != null) {
                nativeSearchQueryBuilder.withAggregations(abstractAggregationBuilder);
            }
            if (pageable != null) {
                nativeSearchQueryBuilder.withPageable(pageable);
            }
            if (fields != null && fields.length > 0) {
                nativeSearchQueryBuilder.withSourceFilter(new FetchSourceFilter(fields, null));
                //nativeSearchQueryBuilder.withFields(fields);
            }
//            nativeSearchQueryBuilder.withSorts(SortBuilders.fieldSort("id").order(SortOrder.ASC));
            return search(clazz, nativeSearchQueryBuilder, nonTenantMode);
        }
        return null;
    }

    @Override
    public String delIndexDoc(String id, Class<?> clazz) {
        return delIndexDoc(id, clazz, false);
    }

    @Override
    public String delIndexDoc(String id, Class<?> clazz, boolean nonTenantMode) {
        return elasticsearchRestTemplate.delete(id, buildIndexCoordinates(clazz, nonTenantMode));
    }

    @Override
    public <T> String delIndexDoc(T model) {
        return delIndexDoc(model, false);
    }

    @Override
    public <T> String delIndexDoc(T model, boolean nonTenantMode) {
        return delIndexDoc(EsReflectUtils.getDocumentIdValue(model), model.getClass(), nonTenantMode);
    }

    /**
     * 根据ID批量删除
     * 官方未提供根据id批量删除的，暂时就以循环删除的方式来操作，若有大批量操作存在性能问题考虑转为query delete方式
     *
     * @param clazz
     * @param ids
     * @return 返回每个ID删除后的返回结果
     */
    @Override
    public List<String> bulkDelIndexDoc(Class<?> clazz, List<String> ids) {
        return bulkDelIndexDoc(clazz, ids, false);
    }

    /**
     * 根据ID批量删除
     * 官方未提供根据id批量删除的，暂时就以循环删除的方式来操作，若有大批量操作存在性能问题考虑转为query delete方式
     *
     * @param clazz
     * @param ids
     * @param nonTenantMode 是否是租户模式，true表示非租户模式，即通用索引
     * @return 返回每个ID删除后的返回结果
     */
    @Override
    public List<String> bulkDelIndexDoc(Class<?> clazz, List<String> ids, boolean nonTenantMode) {
        if (clazz == null || CollectionUtils.isEmpty(ids)) {
            return null;
        }
        List delResutList = new ArrayList();
        for (String id : ids) {
            delResutList.add(elasticsearchRestTemplate.delete(id, buildIndexCoordinates(clazz, nonTenantMode)));
        }
        return delResutList;
    }

}

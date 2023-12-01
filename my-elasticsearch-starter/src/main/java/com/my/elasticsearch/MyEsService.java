package com.my.elasticsearch;

import java.util.List;

import javax.annotation.Nullable;

import com.my.elasticsearch.model.MyEsSearchRequest;
import org.elasticsearch.index.query.QueryBuilder;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.IndexedObjectInformation;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.BulkOptions;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.data.elasticsearch.core.query.UpdateResponse;

/**
 * es服务接口，该接口提供对es的增删改查操作
 *
 * @authro nantian
 * @date 2022-10-08 15:19
 */
public interface MyEsService {
    /**
     * 判断索引是否存在, 文档需标注@Document注解
     *
     * @param clazz
     * @return
     */
    boolean existIndex(Class<?> clazz);

    /**
     * 判断索引是否存在, 文档需标注@Document注解
     *
     * @param clazz
     * @param nonTenantMode 是否是租户模式，true表示非租户模式，即通用索引
     * @return
     */
    boolean existIndex(Class<?> clazz, boolean nonTenantMode);

    /**
     * 创建索引并设置mapping,setting信息
     * 文档需标注@Document注解、包含@Id注解，其它属性字段需要添加@Field注解
     *
     * @param clazz
     * @return
     */
    boolean createIndexIfNotExist(Class<?> clazz);

    /**
     * 创建索引并设置mapping,setting信息
     * 文档需标注@Document注解、包含@Id注解，其它属性字段需要添加@Field注解
     *
     * @param clazz
     * @param nonTenantMode 是否是租户模式，true表示非租户模式，即通用索引
     * @return
     */
    boolean createIndexIfNotExist(Class<?> clazz, boolean nonTenantMode);

    /**
     * 更新索引mapping信息，已存在的索引重复调用新加的字段会自动更新上去，老字段不会变化
     *
     * @param clazz
     * @return
     */
    boolean updateIndexMapping(Class<?> clazz);

    /**
     * 更新索引mapping信息，已存在的索引重复调用新加的字段会自动更新上去，老字段不会变化
     *
     * @param clazz
     * @param nonTenantMode 是否是租户模式，true表示非租户模式，即通用索引
     * @return
     */
    boolean updateIndexMapping(Class<?> clazz, boolean nonTenantMode);

    /**
     * 删除索引，业务应用中不建议用，如果有必要联系管理员在Kibana控台进行操作
     *
     * @param clazz
     * @return
     */
    boolean deleteIndexIfExist(Class<?> clazz);

    /**
     * 删除索引，业务应用中不建议用，如果有必要联系管理员在Kibana控台进行操作
     *
     * @param clazz
     * @param nonTenantMode 是否是租户模式，true表示非租户模式，即通用索引
     * @return
     */
    boolean deleteIndexIfExist(Class<?> clazz, boolean nonTenantMode);

    /**
     * 判断一个文档是否存在
     *
     * @param clazz
     * @param docId
     * @return
     */
    boolean existDocById(Class<?> clazz, String docId);

    /**
     * 判断一个文档是否存在
     *
     * @param clazz
     * @param docId
     * @param nonTenantMode 是否是租户模式，true表示非租户模式，即通用索引
     * @return
     */
    boolean existDocById(Class<?> clazz, String docId, boolean nonTenantMode);

    /**
     * 添加一个数据到索引中，推荐使用@addIndexDoc(T model)
     *
     * @param indexName 索引名
     * @param model     索引数据，注解@Id的字段值不允许为空
     * @return 文档ID
     */
    <T> String addIndexDoc(String indexName, T model);

    /**
     * 添加一个数据到索引中，推荐使用@addIndexDoc(T model)
     *
     * @param indexName     索引名
     * @param model         索引数据，注解@Id的字段值不允许为空
     * @param nonTenantMode 是否是租户模式，true表示非租户模式，即通用索引
     * @return 文档ID
     */
    <T> String addIndexDoc(String indexName, T model, boolean nonTenantMode);

    /**
     * 添加一个数据到索引中
     * 会自动获取类上的@Document(indexName)属性当索引名
     *
     * @param model 文档数据，注解@Id的字段值不允许为空
     * @return
     */
    <T> String addIndexDoc(T model);

    /**
     * 添加一个数据到索引中
     * 会自动获取类上的@Document(indexName)属性当索引名
     *
     * @param model         文档数据，注解@Id的字段值不允许为空
     * @param nonTenantMode 是否是租户模式，true表示非租户模式，即通用索引
     * @return
     */
    <T> String addIndexDoc(T model, boolean nonTenantMode);

    /**
     * 添加一个数据到索引中，指定数据版本号
     *
     * @param model   es文档; 文档需标注@Document注解、包含@Id注解字段, 且@Id注解标注的文档ID字段值不能为空
     * @param version 数据版本号
     * @return
     */
    <T> String saveIndexDoc(T model, Long version);

    /**
     * 添加一个数据到索引中，指定数据版本号
     *
     * @param model         es文档; 文档需标注@Document注解、包含@Id注解字段, 且@Id注解标注的文档ID字段值不能为空
     * @param version       数据版本号
     * @param nonTenantMode 是否是租户模式，true表示非租户模式，即通用索引
     * @return
     */
    <T> String saveIndexDoc(T model, Long version, boolean nonTenantMode);

    /**
     * 添加一个数据到索引中
     * 会自动获取类上的@Document(indexName)属性当索引名
     * 指定数据版本号
     *
     * @param indexName 索引名称
     * @param model     es文档; 文档需标注@Document注解、包含@Id注解字段, 且@Id注解标注的文档ID字段值不能为空
     * @param version   数据版本号
     * @return
     */
    <T> String saveIndexDoc(String indexName, T model, Long version);

    /**
     * 添加一个数据到索引中
     * 会自动获取类上的@Document(indexName)属性当索引名
     * 指定数据版本号
     *
     * @param indexName     索引名称
     * @param model         es文档; 文档需标注@Document注解、包含@Id注解字段, 且@Id注解标注的文档ID字段值不能为空
     * @param version       数据版本号
     * @param nonTenantMode 是否是租户模式，true表示非租户模式，即通用索引
     * @return
     */
    <T> String saveIndexDoc(String indexName, T model, Long version, boolean nonTenantMode);

    /**
     * 批量添加索引，推荐使用@bulkAddIndexDoc(Class<?> clazz, List<T> docList)
     *
     * @param indexName
     * @param docList
     * @return
     */
    <T> List<IndexedObjectInformation> bulkAddIndexDoc(String indexName, List<T> docList);

    /**
     * 批量添加索引，推荐使用@bulkAddIndexDoc(Class<?> clazz, List<T> docList)
     *
     * @param indexName
     * @param docList
     * @param nonTenantMode 是否是租户模式，true表示非租户模式，即通用索引
     * @return
     */
    <T> List<IndexedObjectInformation> bulkAddIndexDoc(String indexName, List<T> docList, boolean nonTenantMode);

    /**
     * 批量添加索引
     *
     * @param indexName 索引名称
     * @param docList   es文档集合; 文档需标注@Document注解、包含@Id、@Version注解字段, 且@Id注解标注的文档ID字段值不能为空、@Version注解标注的文档数据版本字段值不能为空
     * @return
     */
    <T> List<IndexedObjectInformation> bulkSaveIndexDoc(String indexName, List<T> docList);

    /**
     * 批量添加索引
     *
     * @param indexName     索引名称
     * @param docList       es文档集合; 文档需标注@Document注解、包含@Id、@Version注解字段, 且@Id注解标注的文档ID字段值不能为空、@Version注解标注的文档数据版本字段值不能为空
     * @param nonTenantMode 是否是租户模式，true表示非租户模式，即通用索引
     * @return
     */
    <T> List<IndexedObjectInformation> bulkSaveIndexDoc(String indexName, List<T> docList, boolean nonTenantMode);

    /**
     * 批量添加索引，会自动获取类上的 @Document(indexName)属性当索引名
     *
     * @param clazz
     * @param docList
     * @return
     */
    <T> List<IndexedObjectInformation> bulkAddIndexDoc(Class<?> clazz, List<T> docList);

    /**
     * 批量添加索引，会自动获取类上的 @Document(indexName)属性当索引名
     *
     * @param clazz
     * @param docList
     * @param nonTenantMode 是否是租户模式，true表示非租户模式，即通用索引
     * @return
     */
    <T> List<IndexedObjectInformation> bulkAddIndexDoc(Class<?> clazz, List<T> docList, boolean nonTenantMode);

    /**
     * 批量添加索引
     *
     * @param clazz   会自动获取类上的 @Document(indexName)属性当索引名
     * @param docList es文档集合; 文档需标注@Document注解、包含@Id、@Version注解字段, 且@Id注解标注的文档ID字段值不能为空、@Version注解标注的文档数据版本字段值不能为空
     * @return
     */
    <T> List<IndexedObjectInformation> bulkSaveIndexDoc(Class<?> clazz, List<T> docList);

    /**
     * 批量添加索引
     *
     * @param clazz         会自动获取类上的 @Document(indexName)属性当索引名
     * @param docList       es文档集合; 文档需标注@Document注解、包含@Id、@Version注解字段, 且@Id注解标注的文档ID字段值不能为空、@Version注解标注的文档数据版本字段值不能为空
     * @param nonTenantMode 是否是租户模式，true表示非租户模式，即通用索引
     * @return
     */
    <T> List<IndexedObjectInformation> bulkSaveIndexDoc(Class<?> clazz, List<T> docList, boolean nonTenantMode);

    /**
     * 更新文档，会自动获取类上的@Document(indexName)属性当索引名
     *
     * @param model 注解@Id的字段值不允许为空
     * @return
     */
    <T> UpdateResponse.Result updateDoc(T model);

    /**
     * 更新文档，会自动获取类上的@Document(indexName)属性当索引名
     *
     * @param model         注解@Id的字段值不允许为空
     * @param nonTenantMode 是否是租户模式，true表示非租户模式，即通用索引
     * @return
     */
    <T> UpdateResponse.Result updateDoc(T model, boolean nonTenantMode);

    /**
     * 批量更新文档，会自动获取类上的@Document(indexName)属性当索引名
     *
     * @param clazz
     * @param <T>   注解@Id的字段值不允许为空
     * @return
     */
    <T> List<IndexedObjectInformation> bulkUpdateDoc(Class<?> clazz, List<T> modelList);

    /**
     * 批量更新文档
     *
     * @param clazz
     * @param <T>         注解@Id的字段值不允许为空
     * @param bulkOptions
     * @return
     */
    <T> List<IndexedObjectInformation> bulkUpdateDoc(Class<?> clazz, List<T> modelList, BulkOptions bulkOptions);

    /**
     * 根据ID删除一个索引文档
     *
     * @param id
     * @param clazz
     * @return
     */
    String delIndexDoc(String id, Class<?> clazz);

    /**
     * 根据ID删除一个索引文档
     *
     * @param id
     * @param clazz
     * @param nonTenantMode 是否是租户模式，true表示非租户模式，即通用索引
     * @return
     */
    String delIndexDoc(String id, Class<?> clazz, boolean nonTenantMode);

    /**
     * 批量删除索引
     *
     * @param clazz
     * @param ids
     * @return
     */
    List<String> bulkDelIndexDoc(Class<?> clazz, List<String> ids);

    /**
     * 批量删除索引
     *
     * @param clazz
     * @param ids
     * @param nonTenantMode 是否是租户模式，true表示非租户模式，即通用索引
     * @return
     */
    List<String> bulkDelIndexDoc(Class<?> clazz, List<String> ids, boolean nonTenantMode);

    /**
     * 删除一个索引文档，会自动从类上获取注解为@Id属性的value当作ID
     *
     * @param model
     * @param <T>
     * @return
     */
    <T> String delIndexDoc(T model);

    /**
     * 删除一个索引文档，会自动从类上获取注解为@Id属性的value当作ID
     *
     * @param model
     * @param nonTenantMode 是否是租户模式，true表示非租户模式，即通用索引
     * @return
     */
    <T> String delIndexDoc(T model, boolean nonTenantMode);

    /**
     * @param docId
     * @param tClass
     * @param <T>
     * @return
     */
    <T> T findById(String docId, Class<T> tClass);

    /**
     * @param docId
     * @param clazz
     * @param nonTenantMode 是否是租户模式，true表示非租户模式，即通用索引
     * @param <T>
     * @return
     */
    <T> T findById(String docId, Class<T> clazz, boolean nonTenantMode);

    /**
     * 根据ID批量查询
     *
     *  使用id查询数据实时性更好
     *
     * @param indexName
     * @param clazz
     * @param docIdList
     * @param nonTenantMode
     * @param <T>
     * @return
     */
     <T> List<T> findByIds(String indexName, Class<T> clazz, List<String> docIdList, boolean nonTenantMode) ;
     <T> List<T> findByIds(Class<T> clazz, List<String> docIdList) ;
     <T> List<T> findByIds(Class<T> clazz, List<String> docIdList,boolean nonTenantMode) ;


    /**
     * 更丰富灵活的索引查询，开放spring-boot-es-starter原生NativeSearchQueryBuilder
     *
     * @param clazz        自动获取类上的@Document(indexName)属性当索引名
     * @param queryBuilder
     * @return
     */
    <T> SearchHits<T> search(Class<T> clazz, NativeSearchQueryBuilder queryBuilder);

    /**
     * 更丰富灵活的索引查询，开放spring-boot-es-starter原生NativeSearchQueryBuilder
     *
     * @param clazz         自动获取类上的@Document(indexName)属性当索引名
     * @param queryBuilder
     * @param nonTenantMode 是否是租户模式，true表示非租户模式，即通用索引
     * @return
     */
    <T> SearchHits<T> search(Class<T> clazz, NativeSearchQueryBuilder queryBuilder, boolean nonTenantMode);

    /**
     * 封装查询对象，简化NativeSearchQueryBuilder构造过程
     *
     * @param clazz   自动获取类上的@Document(indexName)属性当索引名
     * @param request
     * @return
     */
    <T> SearchHits<T> search(Class<T> clazz, MyEsSearchRequest request);

    /**
     * 封装查询对象，简化NativeSearchQueryBuilder构造过程
     *
     * @param clazz         自动获取类上的@Document(indexName)属性当索引名
     * @param request
     * @param nonTenantMode 是否是租户模式，true表示非租户模式，即通用索引
     * @return
     */
    <T> SearchHits<T> search(Class<T> clazz, MyEsSearchRequest request, boolean nonTenantMode);

    /**
     * 精确查询类场景推荐使用，es不会计算文档相关性分值，性能更好
     *
     * @param clazz         自动获取类上的@Document(indexName)属性当索引名
     * @param filterBuilder
     * @param pageable
     * @return
     */
    <T> SearchHits<T> searchByFilter(Class<T> clazz, QueryBuilder filterBuilder, @Nullable Pageable pageable);

    /**
     * 精确查询类场景推荐使用，es不会计算文档相关性分值，性能更好
     *
     * @param clazz         自动获取类上的@Document(indexName)属性当索引名
     * @param filterBuilder
     * @param pageable
     * @param nonTenantMode 是否是租户模式，true表示非租户模式，即通用索引
     * @return
     */
    <T> SearchHits<T> searchByFilter(Class<T> clazz, QueryBuilder filterBuilder, @Nullable Pageable pageable, boolean nonTenantMode);

    /**
     * 标题或文章内容检索类场景推荐使用，es会计算文档相关性，并按相关性自动排序
     *
     * @param clazz
     * @param queryBuilder
     * @param pageable
     * @return
     */
    <T> SearchHits<T> search(Class<T> clazz, QueryBuilder queryBuilder, @Nullable Pageable pageable);

    /**
     * 标题或文章内容检索类场景推荐使用，es会计算文档相关性，并按相关性自动排序
     *
     * @param clazz
     * @param queryBuilder
     * @param pageable
     * @param nonTenantMode 是否是租户模式，true表示非租户模式，即通用索引
     * @return
     */
    <T> SearchHits<T> search(Class<T> clazz, QueryBuilder queryBuilder, @Nullable Pageable pageable, boolean nonTenantMode);

    /**
     * 索引数据查询
     *
     * @param clazz         索引类
     * @param queryBuilder
     * @param filterBuilder
     * @return
     */
    <T> SearchHits<T> search(Class<T> clazz, QueryBuilder queryBuilder, QueryBuilder filterBuilder, @Nullable Pageable pageable);

    /**
     * 索引数据查询
     *
     * @param clazz         索引类
     * @param queryBuilder
     * @param filterBuilder
     * @param nonTenantMode 是否是租户模式，true表示非租户模式，即通用索引
     * @return
     */
    <T> SearchHits<T> search(Class<T> clazz, QueryBuilder queryBuilder, QueryBuilder filterBuilder, @Nullable Pageable pageable, boolean nonTenantMode);

}

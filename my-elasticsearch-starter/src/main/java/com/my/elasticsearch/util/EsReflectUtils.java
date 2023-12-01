package com.my.elasticsearch.util;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.my.elasticsearch.model.RelationModel;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.lang.NonNull;
import org.springframework.util.Assert;

/**
 * 反射工具类
 *
 * @author nantian
 * @since 2022/10/08 17:33
 */
public class EsReflectUtils extends cn.hutool.core.util.ReflectUtil {


    /**
     * 根据指定的注解获取标注了注解的字段
     *
     * @param targetClass     目标对象Class
     * @param annotationClass 注解Class
     * @return
     */
    public static List<Field> getClassFieldsByAnnotation(Class<?> targetClass,
                                                         Class<? extends Annotation> annotationClass) {
        if (Objects.nonNull(targetClass) && Objects.nonNull(annotationClass)) {
            Field[] fields = getFields(targetClass);
            if (Objects.nonNull(fields) && fields.length > 0) {
                List<Field> response = new ArrayList<>();
                for (Field field : fields) {
                    Annotation annotation = field.getAnnotation(annotationClass);
                    if (Objects.nonNull(annotation)) {
                        response.add(field);
                    }
                }
                return response.isEmpty() ? null : response;
            }
        }
        return null;
    }

    /**
     * 根据指定Type获取字段
     *
     * @param targetClass 目标对象Class
     * @param type        类型
     * @return
     */
    public static List<Field> getClassFieldsByType(Class<?> targetClass, Type type) {
        if (Objects.nonNull(targetClass) && Objects.nonNull(type)) {
            Field[] fields = getFields(targetClass);
            if (Objects.nonNull(fields) && fields.length > 0) {
                List<Field> response = new ArrayList<>();
                for (Field field : fields) {
                    if (field.getType().equals(type)) {
                        response.add(field);
                    }
                }
                return response.isEmpty() ? null : response;
            }
        }
        return null;
    }

    /**
     * 获取spring-es document注解配置值
     *
     * @param clazz
     * @return
     */
    public static String getDocumentIndexName(Class<?> clazz) {
        Document[] annotation = clazz.getAnnotationsByType(Document.class);
        if (annotation == null || annotation[0] == null) {
            throw new NullPointerException(clazz.getName() + " annotation @Document is empty");
        }
        Document document = annotation[0];
        String indexName = document.indexName();
        return indexName;
    }

    public static void setAnnotationFieldVal(Class<?> clazz) {
        try {
            Document document = clazz.getAnnotation(Document.class);
            InvocationHandler invocationHandler = Proxy.getInvocationHandler(document);
            Field field = invocationHandler.getClass().getDeclaredField("memberValues");
            field.setAccessible(true);
            Map memberValues = (Map) field.get(invocationHandler);
            //修改属性值
            memberValues.put("indexName", EsTenantUtil.getTenantCode() + "." + memberValues.get("indexName"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 获取elasticsearch bean 标注@Id注解的文档Id字段名称
     * 校验 elasticsearch bean 是否实现了@Document注解
     * 获取标注了@Id注解的字段(存在多个取first)
     *
     * @param tClass
     * @param <T>
     * @return 文档Id字段名称 not null
     */
    @NonNull
    public static <T> String getDocumentIdFieldName(Class<T> tClass) {
        validDocument(tClass);
        List<Field> fields = EsReflectUtils.getClassFieldsByAnnotation(tClass, Id.class);
        // notEmpty 已校验notNull, 但是编译器无法检测NPE; 添加此句抑制编译器
        Assert.notNull(fields,
                tClass.getSimpleName() + " no fields marked with @" + Id.class.getName() + " annotation.");
        Assert.notEmpty(fields,
                tClass.getSimpleName() + " no fields marked with @" + Id.class.getName() + " annotation.");
        return fields.get(0).getName();
    }

    /**
     * 校验JavaBean是否实现了@Document注解
     *
     * @param elasticsearchModel elasticsearch bean
     * @param <T>
     */
    public static <T> void validDocument(T elasticsearchModel) {
        Assert.notNull(elasticsearchModel, elasticsearchModel.getClass().getSimpleName() + " must not be null.");
        validDocument(elasticsearchModel.getClass());
    }

    /**
     * 校验JavaBean是否实现了@Document注解
     *
     * @param tClass elasticsearch bean class
     * @param <T>
     */
    public static <T> void validDocument(Class<T> tClass) {
        Assert.notNull(tClass, tClass.getSimpleName() + " must not be null.");
        org.springframework.data.elasticsearch.annotations.Document document = tClass
                .getAnnotation(org.springframework.data.elasticsearch.annotations.Document.class);
        Assert.notNull(document, tClass.getSimpleName() + " must have @"
                + org.springframework.data.elasticsearch.annotations.Document.class.getName() + " annotation.");
    }

    /**
     * 获取elasticsearch bean 标注@Id注解的文档Id值
     * 校验 elasticsearch bean 是否实现了@Document注解
     * 获取标注了@Id注解的字段(存在多个取first)
     *
     * @param elasticsearchModel
     * @param <T>
     * @return 文档Id值; not null
     */
    @NonNull
    public static <T> String getDocumentIdValue(T elasticsearchModel) {
        EsReflectUtils.validDocument(elasticsearchModel);
        List<Field> fields = EsReflectUtils.getClassFieldsByAnnotation(elasticsearchModel.getClass(), Id.class);
        // notEmpty 已校验notNull, 但是编译器无法检测NPE; 添加此句抑制编译器
        Assert.notNull(fields, elasticsearchModel.getClass().getSimpleName()
                + " no fields marked with @" + Id.class.getName() + " annotation.");
        Assert.notEmpty(fields, elasticsearchModel.getClass().getSimpleName()
                + " no fields marked with @" + Id.class.getName() + " annotation.");
        Object fieldValue = EsReflectUtils.getFieldValue(elasticsearchModel, fields.get(0));
        Assert.isTrue(fieldValue != null && StringUtils.isNotEmpty(fieldValue.toString()),
                elasticsearchModel.getClass().getSimpleName() + " @Id value must not be null.");
        return String.valueOf(fieldValue);
    }

    /**
     * 获取elasticsearch bean属性为`RelationModel.class`字段的parent父文档ID字段值
     * 校验 elasticsearch bean 是否实现了@Document注解
     * 获取属性为`RelationModel.class`的字段, 多个根据指定的子文档类型名称获取其对应
     * 获取 RelationModel.parent 父文档ID字段值返回
     *
     * @param elasticsearchModel
     * @param subRelationName    子文档关系名
     * @param <T>
     * @return 父文档ID
     */
    @NonNull
    public static <T> String getDocumentParentIdValue(T elasticsearchModel, String subRelationName) {
        EsReflectUtils.validDocument(elasticsearchModel);
        Assert.isTrue(StringUtils.isNotEmpty(subRelationName), "parameter `subRelationName` must not be null");
        List<Field> fields = EsReflectUtils.getClassFieldsByType(elasticsearchModel.getClass(), RelationModel.class);
        // notEmpty 已校验notNull, 但是编译器无法检测NPE; 添加此句抑制编译器
        Assert.notNull(fields,
                elasticsearchModel.getClass().getSimpleName() + " must has " + RelationModel.class.getName() + " fields.");
        Assert.notEmpty(fields,
                elasticsearchModel.getClass().getSimpleName() + " must has " + RelationModel.class.getName() + " fields.");
        for (Field field : fields) {
            Method getMethod = EsReflectUtils.getMethodByName(elasticsearchModel.getClass(),
                    //TODO
                    //StringUtils.upperFirstAndAddPre(field.getName(), "get")
                    "get" + field.getName()
            );
            Assert.notNull(getMethod, elasticsearchModel.getClass().getSimpleName() + " must has " + field.getName()
                    + " field getter method.");
            try {
                RelationModel relationModel = (RelationModel) getMethod.invoke(elasticsearchModel);
                Assert.notNull(relationModel, elasticsearchModel.getClass().getSimpleName() + "." + field.getName()
                        + " field value must not be null.");
                if (relationModel.getName().equals(subRelationName)) {
                    Assert.isTrue(StringUtils.isNotEmpty(relationModel.getParent()),
                            elasticsearchModel.getClass().getSimpleName() + "." + field.getName()
                                    + ".parent value must not be null.");
                    return relationModel.getParent();
                }
            } catch (IllegalAccessException | InvocationTargetException e) {
                e.printStackTrace();
            }
        }
        throw new IllegalArgumentException(
                elasticsearchModel.getClass().getSimpleName() + " has no sub relation model filed with name eq '"
                        + subRelationName + "'");
    }

    /**
     * 获取elasticsearch bean 标注@Version注解的文档Version值
     * 校验 elasticsearch bean 是否实现了@Document注解
     * 获取标注了@Version注解的字段(存在多个取first)
     *
     * @param elasticsearchModel
     * @param <T>
     * @return 文档Version值; not null
     */
    @NonNull
    public static <T> Long getDocumentVersionValue(T elasticsearchModel) {
        EsReflectUtils.validDocument(elasticsearchModel);
        List<Field> fields = EsReflectUtils.getClassFieldsByAnnotation(elasticsearchModel.getClass(), Version.class);
        // notEmpty 已校验notNull, 但是编译器无法检测NPE; 添加此句抑制编译器
        Assert.notNull(fields, elasticsearchModel.getClass().getSimpleName()
                + " no fields marked with @" + Version.class.getName() + " annotation.");
        Assert.notEmpty(fields, elasticsearchModel.getClass().getSimpleName()
                + " no fields marked with @" + Version.class.getName() + " annotation.");
        Object fieldValue = EsReflectUtils.getFieldValue(elasticsearchModel, fields.get(0));
        Assert.isTrue(fieldValue != null && StringUtils.isNotEmpty(fieldValue.toString()),
                elasticsearchModel.getClass().getSimpleName() + " @Version value must not be null.");
        return Long.valueOf(fieldValue.toString());
    }

}

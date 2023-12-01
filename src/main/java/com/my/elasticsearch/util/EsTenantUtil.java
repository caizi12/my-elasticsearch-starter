package com.my.elasticsearch.util;


/**
 * es 租户util
 *
 * @authro nantian
 * @date 2022-10-09 19:31
 */
public class EsTenantUtil {
    /**
     * 获取租户模式下索引名
     *
     * @param index
     * @return
     */
    public static String getTenantIndex(String index) {
        //多租户下，可以添加租户前缀
        //return getTenantCode() + "." + index;
        return index;
    }

    public static String getTenantCode() {
        //TODO 自行扩展
        return "";
    }

}

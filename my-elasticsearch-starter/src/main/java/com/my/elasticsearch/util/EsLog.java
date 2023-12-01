package com.my.elasticsearch.util;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * es日志
 * 
 * @authro nantian
 * @date 2022-10-28 10:17
 */
public class EsLog {
    private static final Logger LOGGER = LoggerFactory.getLogger(EsLog.class);

    public static Logger getLogger() {
        return LOGGER;
    }

    public static void log(String msg) {
        if (StringUtils.isNotBlank(msg)) {
            if (getLogger() != null) {
                warn(msg);
            } else {
                System.out.println(msg);
            }

        }
    }

    public static void log(String msg,Throwable t) {
        if (StringUtils.isNotBlank(msg)) {
            if (getLogger() != null) {
                warn(msg,t);
            } else {
                System.out.println(msg);
                if(t != null){
                    t.printStackTrace();
                }
            }

        }
    }

    public static void warn(String msg) {
        getLogger().warn(msg);
    }

    public static void warn(String msg,Throwable t) {
        getLogger().warn(msg,t);
    }

    public static void debug(String msg) {
        getLogger().debug(msg);
    }

}

package com.web.common;

import com.web.common.constant.InterConstant;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

/**
 * <p>简要说明...</p>
 *
 * @author JingHe
 * @version 1.0
 * @since 2019/8/27
 */
public class CommonUtil {


    /**
     * 对象转map
     * @param obj 对象
     * @param isNull 是否包含为null属性
     * @return 转换后map
     */
    public static Map<String, Object> objectToMap(Object obj, boolean isNull) {
        Map<String, Object> map = new HashMap<>(16);
        Class clazz = obj.getClass();
        Field[] fields = clazz.getDeclaredFields();
        for (Field field: fields) {
            filedToMap(obj, field, map, isNull);
        }
        Class superClazz = clazz.getSuperclass();
        Field[] superFields = superClazz.getDeclaredFields();
        for (Field field: superFields) {
            filedToMap(obj, field, map, isNull);
        }
        return map;
    }


    /**
     * 对象转换http get请求
     * @param obj 对象
     * @param isNull 是否包含为null字段
     * @return 参数url
     */
    public static String objectToHttpParam(Object obj, boolean isNull) {
        if (obj == null) return null;
        Class clazz = obj.getClass();
        StringBuilder sb = new StringBuilder();
        Field[] fields = clazz.getDeclaredFields();
        for (Field field: fields) {
            fieldToHttpString(obj, field, sb, isNull);
        }
        Class superClazz = clazz.getSuperclass();
        Field[] superFields = superClazz.getDeclaredFields();
        for (Field field: superFields) {
            fieldToHttpString(obj, field, sb, isNull);
        }
        return sb.toString();
    }

    /**
     * 对象属性转换成map
     * @param obj 对象
     * @param field 属性
     * @param map map
     * @param isNull 是否包含为Null属性
     */
    private static void filedToMap(Object obj, Field field,
                                   Map<String, Object> map, boolean isNull) {
        try {
            field.setAccessible(true);
            Object value = field.get(obj);
            if (isNull || value != null) {
                map.put(field.getName(), value);
            }
        }
        catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * 对象属性拼接http get请求
     * @param obj obj
     * @param field 属性
     * @param sb 字符串
     * @param isNull 是否包含为Null属性
     */
    private static void fieldToHttpString(Object obj, Field field, StringBuilder sb, boolean isNull) {
        try {
            field.setAccessible(true);
            Object value = field.get(obj);
            if (isNull || value != null) {
                if (sb.length() > 0) {
                    sb.append(InterConstant.AND_SIGN_TITLE);
                }
                sb.append(field.getName()).append(InterConstant.EQUIVALENT_SIGN)
                        .append(value);
            }
        }
        catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }
}

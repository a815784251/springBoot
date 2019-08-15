package com.web.dal.annotation;

import java.lang.annotation.*;
import java.sql.PreparedStatement;

/**
 * 用于标注table类的字段和数据库表字段之间的对应关系
 *
 * @since 2013-3-6
 * @author Jonn
 */
@Inherited
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface DbField {

    /**
     * 类字段对应的表的列字段名，默认是使用类字段名
     */
    public String name() default "";

    /**
     * <pre>
     * 在生成sql语句时，为true表示将通过{@link PreparedStatement}来设置值，为false表示将值直接生成到sql语句里面，用于适应一些特殊情况，默认为true
     * 
     * 以字段名为name为例，值为yafw
     * 
     * 如果本参数填true，生成的sql为 name=?, 通过setObject将yafw字符串设置进去
     * 如果本参数填false，生成的sql为 name='yafw'，直接构成了sql，不走setObject等
     * </pre>
     */
    public boolean preparedStatementArg() default true;

    /**
     * 如果不需要该字段的值写入数据库，请设置为false，设置为false时自动生成insert/updated sql的机制都会忽略该字段
     */
    boolean writeToDb() default true;

    /**
     * 如果不需要从数据库读取该字段的值，请设置为false
     */
    boolean readFromDb() default true;
}

package com.web.dal.exception;

import org.springframework.dao.DataAccessException;

/**
 * 进行数据库对象操作时，处理JavaBean时遇到的异常
 *
 * @since 2011-9-5
 * @author Jonn
 */
public class BeanProcessException extends DataAccessException {

    private static final long serialVersionUID = -7362736711045963966L;

    public BeanProcessException(String msg) {
        super(msg);
    }

    public BeanProcessException(String msg, Throwable cause) {
        super(msg, cause);
    }
}

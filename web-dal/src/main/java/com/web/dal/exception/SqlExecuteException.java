package com.web.dal.exception;

import org.springframework.dao.DataAccessException;

/**
 * 进行数据库对象操作时，执行sql时遇到的异常
 *
 * @since 2015-03-02
 * @author Jonn
 */
public class SqlExecuteException extends DataAccessException {

    private static final long serialVersionUID = 6997110205156002766L;

    public SqlExecuteException(String msg) {
        super(msg);
    }

    public SqlExecuteException(String msg, Throwable cause) {
        super(msg, cause);
    }
}

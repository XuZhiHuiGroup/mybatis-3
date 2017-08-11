/**
 * Copyright 2009-2015 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.ibatis.executor.statement;

import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.executor.ExecutorException;
import org.apache.ibatis.executor.parameter.ParameterHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

/**
 * @author Clinton Begin
 */
@Slf4j
public class RoutingStatementHandler implements StatementHandler {

    private final StatementHandler delegate;

    public RoutingStatementHandler(Executor executor, MappedStatement ms, Object parameter, RowBounds rowBounds, ResultHandler resultHandler, BoundSql boundSql) {
        log.debug("RoutingStatementHandler({},{},{},{},{},{})", executor, ms, parameter, rowBounds, resultHandler, boundSql);
        switch (ms.getStatementType()) {
            case STATEMENT:
                delegate = new SimpleStatementHandler(executor, ms, parameter, rowBounds, resultHandler, boundSql);
                break;
            case PREPARED:
                delegate = new PreparedStatementHandler(executor, ms, parameter, rowBounds, resultHandler, boundSql);
                break;
            case CALLABLE:
                delegate = new CallableStatementHandler(executor, ms, parameter, rowBounds, resultHandler, boundSql);
                break;
            default:
                throw new ExecutorException("Unknown statement type: " + ms.getStatementType());
        }

    }

    @Override
    public Statement prepare(Connection connection) throws SQLException {
        log.debug("prepare({})", connection);
        return delegate.prepare(connection);
    }

    @Override
    public void parameterize(Statement statement) throws SQLException {
        log.debug("parameterize({})", statement);
        delegate.parameterize(statement);
    }

    @Override
    public void batch(Statement statement) throws SQLException {
        log.debug("batch({})", statement);
        delegate.batch(statement);
    }

    @Override
    public int update(Statement statement) throws SQLException {
        log.debug("update({})", statement);
        return delegate.update(statement);
    }

    @Override
    public <E> List<E> query(Statement statement, ResultHandler resultHandler) throws SQLException {
        log.debug("query({},{})", statement, resultHandler);
        return delegate.<E>query(statement, resultHandler);
    }

    @Override
    public BoundSql getBoundSql() {
        log.debug("getBoundSql()");
        return delegate.getBoundSql();
    }

    @Override
    public ParameterHandler getParameterHandler() {
        log.debug("getParameterHandler()");
        return delegate.getParameterHandler();
    }
}

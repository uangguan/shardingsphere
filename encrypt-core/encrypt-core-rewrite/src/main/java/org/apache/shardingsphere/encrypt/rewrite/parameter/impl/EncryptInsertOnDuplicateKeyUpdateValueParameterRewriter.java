/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.shardingsphere.encrypt.rewrite.parameter.impl;

import com.google.common.base.Preconditions;
import lombok.Setter;
import org.apache.shardingsphere.encrypt.rewrite.parameter.EncryptParameterRewriter;
import org.apache.shardingsphere.encrypt.strategy.spi.Encryptor;
import org.apache.shardingsphere.encrypt.strategy.spi.QueryAssistedEncryptor;
import org.apache.shardingsphere.sql.parser.binder.segment.insert.values.OnDuplicateUpdateContext;
import org.apache.shardingsphere.sql.parser.binder.statement.SQLStatementContext;
import org.apache.shardingsphere.sql.parser.binder.statement.dml.InsertStatementContext;
import org.apache.shardingsphere.underlying.rewrite.parameter.builder.ParameterBuilder;
import org.apache.shardingsphere.underlying.rewrite.parameter.builder.impl.GroupedParameterBuilder;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

/**
 * Insert on duplicate key update parameter rewriter for encrypt.
 */
@Setter
public final class EncryptInsertOnDuplicateKeyUpdateValueParameterRewriter extends EncryptParameterRewriter<InsertStatementContext> {
    
    @Override
    protected boolean isNeedRewriteForEncrypt(final SQLStatementContext sqlStatementContext) {
        return sqlStatementContext instanceof InsertStatementContext && ((InsertStatementContext) sqlStatementContext).getSqlStatement().getOnDuplicateKeyColumns().isPresent();
    }
    
    @Override
    public void rewrite(final ParameterBuilder parameterBuilder, final InsertStatementContext insertStatementContext, final List<Object> parameters) {
        String tableName = insertStatementContext.getSqlStatement().getTable().getTableName().getIdentifier().getValue();
        GroupedParameterBuilder groupedParameterBuilder = (GroupedParameterBuilder) parameterBuilder;
        OnDuplicateUpdateContext onDuplicateKeyUpdateValueContext = insertStatementContext.getOnDuplicateKeyUpdateValueContext();
        for (int index = 0; index < onDuplicateKeyUpdateValueContext.getValueExpressions().size(); index++) {
            final int columnIndex = index;
            String encryptLogicColumnName = onDuplicateKeyUpdateValueContext.getColumn(columnIndex).getIdentifier().getValue();
            Optional<Encryptor> encryptorOptional = getEncryptRule().findEncryptor(tableName, encryptLogicColumnName);
            encryptorOptional.ifPresent(encryptor -> {
                Object plainColumnValue = onDuplicateKeyUpdateValueContext.getValue(columnIndex);
                Object cipherColumnValue = encryptorOptional.get().encrypt(plainColumnValue);
                groupedParameterBuilder.getOnDuplicateKeyUpdateParametersBuilder().addReplacedParameters(columnIndex, cipherColumnValue);
                Collection<Object> addedParameters = new LinkedList<>();
                if (encryptor instanceof QueryAssistedEncryptor) {
                    Optional<String> assistedColumnName = getEncryptRule().findAssistedQueryColumn(tableName, encryptLogicColumnName);
                    Preconditions.checkArgument(assistedColumnName.isPresent(), "Can not find assisted query Column Name");
                    addedParameters.add(((QueryAssistedEncryptor) encryptor).queryAssistedEncrypt(plainColumnValue.toString()));
                }

                if (getEncryptRule().findPlainColumn(tableName, encryptLogicColumnName).isPresent()) {
                    addedParameters.add(plainColumnValue);
                }

                if (!addedParameters.isEmpty()) {
                    if (!groupedParameterBuilder.getOnDuplicateKeyUpdateParametersBuilder().getAddedIndexAndParameters().containsKey(columnIndex + 1)) {
                        groupedParameterBuilder.getOnDuplicateKeyUpdateParametersBuilder().getAddedIndexAndParameters().put(columnIndex + 1, new LinkedList<>());
                    }
                    groupedParameterBuilder.getOnDuplicateKeyUpdateParametersBuilder().getAddedIndexAndParameters().get(columnIndex + 1).addAll(addedParameters);
                }
            });
        }
    }
}

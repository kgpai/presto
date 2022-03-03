/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.facebook.presto.execution;

import com.facebook.presto.Session;
import com.facebook.presto.metadata.Metadata;
import com.facebook.presto.security.AccessControl;
import com.facebook.presto.spi.WarningCollector;
import com.facebook.presto.sql.analyzer.SemanticException;
import com.facebook.presto.sql.tree.DropRole;
import com.facebook.presto.sql.tree.Expression;
import com.facebook.presto.transaction.TransactionManager;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.List;
import java.util.Set;

import static com.facebook.presto.metadata.MetadataUtil.createCatalogName;
import static com.facebook.presto.sql.analyzer.SemanticErrorCode.MISSING_ROLE;
import static com.google.common.util.concurrent.Futures.immediateFuture;
import static java.util.Locale.ENGLISH;

public class DropRoleTask
        implements DDLDefinitionTask<DropRole>
{
    @Override
    public String getName()
    {
        return "DROP ROLE";
    }

    @Override
    public ListenableFuture<?> execute(DropRole statement, TransactionManager transactionManager, Metadata metadata, AccessControl accessControl, Session session, List<Expression> parameters, WarningCollector warningCollector)
    {
        String catalog = createCatalogName(session, statement);
        String role = statement.getName().getValue().toLowerCase(ENGLISH);
        accessControl.checkCanDropRole(session.getRequiredTransactionId(), session.getIdentity(), session.getAccessControlContext(), role, catalog);
        Set<String> existingRoles = metadata.listRoles(session, catalog);
        if (!existingRoles.contains(role)) {
            throw new SemanticException(MISSING_ROLE, statement, "Role '%s' does not exist", role);
        }
        metadata.dropRole(session, role, catalog);
        return immediateFuture(null);
    }
}

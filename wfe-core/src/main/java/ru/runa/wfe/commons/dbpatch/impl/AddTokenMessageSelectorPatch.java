package ru.runa.wfe.commons.dbpatch.impl;

import java.sql.Types;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import ru.runa.wfe.commons.Utils;
import ru.runa.wfe.commons.dbpatch.DBPatch;
import ru.runa.wfe.commons.dbpatch.IDbPatchPostProcessor;
import ru.runa.wfe.definition.dao.ProcessDefinitionLoader;
import ru.runa.wfe.execution.ExecutionContext;
import ru.runa.wfe.execution.Token;
import ru.runa.wfe.execution.dao.TokenDAO;
import ru.runa.wfe.lang.BaseMessageNode;
import ru.runa.wfe.lang.ProcessDefinition;

public class AddTokenMessageSelectorPatch extends DBPatch implements IDbPatchPostProcessor {
    @Autowired
    TokenDAO tokenDAO;
    @Autowired
    ProcessDefinitionLoader processDefinitionLoader;

    @Override
    protected List<String> getDDLQueriesBefore() {
        List<String> sql = super.getDDLQueriesBefore();
        sql.add(getDDLCreateColumn("BPM_TOKEN", new ColumnDef("MESSAGE_SELECTOR", dialect.getTypeName(Types.VARCHAR, 1024, 1024, 1024))));
        sql.add(getDDLCreateIndex("BPM_TOKEN", "IX_MESSAGE_SELECTOR", "MESSAGE_SELECTOR"));
        return sql;
    }

    @Override
    public void postExecute() throws Exception {
        List<Token> tokens = tokenDAO.findByMessageSelectorIsNullAndExecutionStatusIsActive();
        log.info("Updating " + tokens.size() + " tokens message selector");
        for (Token token : tokens) {
            ProcessDefinition processDefinition = processDefinitionLoader.getDefinition(token.getProcess());
            BaseMessageNode messageNode = (BaseMessageNode) processDefinition.getNodeNotNull(token.getNodeId());
            ExecutionContext executionContext = new ExecutionContext(processDefinition, token.getProcess());
            String messageSelector = Utils.getReceiveMessageNodeSelector(executionContext.getVariableProvider(), messageNode);
            token.setMessageSelector(messageSelector);
        }
    }
}

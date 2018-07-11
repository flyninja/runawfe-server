package ru.runa.wfe.commons.dbpatch.impl;

import java.sql.Types;
import java.util.List;

import ru.runa.wfe.commons.dbpatch.DbPatch;

public class AddAggregatedTaskIndexPatch extends DbPatch {

    @Override
    protected List<String> getDDLQueriesBefore() {
        List<String> sql = super.getDDLQueriesAfter();
        sql.add(getDDLCreateColumn("BPM_AGGLOG_TASKS", new ColumnDef("TASK_INDEX", dialect.getTypeName(Types.INTEGER))));
        return sql;
    }

}

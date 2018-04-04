/*
 * This file is part of the RUNA WFE project.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation; version 2.1
 * of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.
 */
package ru.runa.wfe.commons.dbpatch;

import com.google.common.collect.Lists;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.tool.hbm2ddl.SchemaExport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import ru.runa.wfe.commons.ApplicationContextFactory;
import ru.runa.wfe.commons.ClassLoaderUtil;
import ru.runa.wfe.commons.DatabaseProperties;
import ru.runa.wfe.commons.PropertyResources;
import ru.runa.wfe.commons.SystemProperties;
import ru.runa.wfe.commons.dao.ConstantDAO;
import ru.runa.wfe.commons.dao.Localization;
import ru.runa.wfe.commons.dao.LocalizationDAO;
import ru.runa.wfe.commons.dbpatch.impl.AddAggregatedTaskIndexPatch;
import ru.runa.wfe.commons.dbpatch.impl.AddAssignDateColumnPatch;
import ru.runa.wfe.commons.dbpatch.impl.AddBatchPresentationIsSharedPatch;
import ru.runa.wfe.commons.dbpatch.impl.AddColumnForEmbeddedBotTaskFileName;
import ru.runa.wfe.commons.dbpatch.impl.AddColumnsToSubstituteEscalatedTasksPatch;
import ru.runa.wfe.commons.dbpatch.impl.AddCreateDateColumns;
import ru.runa.wfe.commons.dbpatch.impl.AddDeploymentAuditPatch;
import ru.runa.wfe.commons.dbpatch.impl.AddDueDateExpressionToJobAndTask;
import ru.runa.wfe.commons.dbpatch.impl.AddEmbeddedFileForBotTask;
import ru.runa.wfe.commons.dbpatch.impl.AddHierarchyProcess;
import ru.runa.wfe.commons.dbpatch.impl.AddMultiTaskIndexToTaskPatch;
import ru.runa.wfe.commons.dbpatch.impl.AddNodeIdToProcessLogPatch;
import ru.runa.wfe.commons.dbpatch.impl.AddParentProcessIdPatch;
import ru.runa.wfe.commons.dbpatch.impl.AddProcessAndTokenExecutionStatusPatch;
import ru.runa.wfe.commons.dbpatch.impl.AddSequentialFlagToBot;
import ru.runa.wfe.commons.dbpatch.impl.AddSettingsTable;
import ru.runa.wfe.commons.dbpatch.impl.AddSubProcessIndexColumn;
import ru.runa.wfe.commons.dbpatch.impl.AddSubprocessBindingDatePatch;
import ru.runa.wfe.commons.dbpatch.impl.AddTitleAndDepartmentColumnsToActorPatch;
import ru.runa.wfe.commons.dbpatch.impl.AddTokenErrorDataPatch;
import ru.runa.wfe.commons.dbpatch.impl.AddTokenMessageSelectorPatch;
import ru.runa.wfe.commons.dbpatch.impl.AddTransactionalBotSupport;
import ru.runa.wfe.commons.dbpatch.impl.AddVariableUniqueKeyPatch;
import ru.runa.wfe.commons.dbpatch.impl.CreateAdminScriptTables;
import ru.runa.wfe.commons.dbpatch.impl.CreateAggregatedLogsTables;
import ru.runa.wfe.commons.dbpatch.impl.CreateReportsTables;
import ru.runa.wfe.commons.dbpatch.impl.ExpandDescriptionsPatch;
import ru.runa.wfe.commons.dbpatch.impl.ExpandVarcharPatch;
import ru.runa.wfe.commons.dbpatch.impl.JbpmRefactoringPatch;
import ru.runa.wfe.commons.dbpatch.impl.NodeTypeChangePatch;
import ru.runa.wfe.commons.dbpatch.impl.PerformancePatch401;
import ru.runa.wfe.commons.dbpatch.impl.PermissionMappingPatch403;
import ru.runa.wfe.commons.dbpatch.impl.TaskCreateLogSeverityChangedPatch;
import ru.runa.wfe.commons.dbpatch.impl.TaskEndDateRemovalPatch;
import ru.runa.wfe.commons.dbpatch.impl.TaskOpenedByExecutorsPatch;
import ru.runa.wfe.commons.dbpatch.impl.TransitionLogPatch;
import ru.runa.wfe.commons.logic.LocalizationParser;
import ru.runa.wfe.definition.dao.IProcessDefinitionLoader;
import ru.runa.wfe.execution.dao.ProcessDAO;
import ru.runa.wfe.execution.dao.TokenDAO;
import ru.runa.wfe.security.dao.PermissionDAO;
import ru.runa.wfe.user.dao.ExecutorDAO;

/**
 * Initial DB population and update during version change.
 * 
 * @author Dofs
 */
public class InitializerLogic implements ApplicationListener<ContextRefreshedEvent> {
    protected static final Log log = LogFactory.getLog(InitializerLogic.class);
    private static final List<Class<? extends DBPatch>> dbPatches;
    @Autowired
    private DbTransactionalInitializer dbTransactionalInitializer;

    static {
        List<Class<? extends DBPatch>> patches = Lists.newArrayList();
        patches.add(UnsupportedPatch.class);
        patches.add(UnsupportedPatch.class);
        patches.add(UnsupportedPatch.class);
        patches.add(UnsupportedPatch.class);
        patches.add(UnsupportedPatch.class);
        patches.add(UnsupportedPatch.class);
        patches.add(UnsupportedPatch.class);
        patches.add(UnsupportedPatch.class);
        patches.add(UnsupportedPatch.class);
        patches.add(UnsupportedPatch.class);
        patches.add(UnsupportedPatch.class);
        patches.add(UnsupportedPatch.class);
        patches.add(UnsupportedPatch.class);
        patches.add(UnsupportedPatch.class);
        patches.add(UnsupportedPatch.class);
        patches.add(UnsupportedPatch.class);
        patches.add(UnsupportedPatch.class);
        patches.add(UnsupportedPatch.class);
        patches.add(UnsupportedPatch.class);
        patches.add(UnsupportedPatch.class);
        // version 20
        // 4.0.0
        patches.add(AddHierarchyProcess.class);
        patches.add(JbpmRefactoringPatch.class);
        patches.add(TransitionLogPatch.class);
        // 4.0.1
        patches.add(PerformancePatch401.class);
        patches.add(TaskEndDateRemovalPatch.class);
        // 4.0.1
        patches.add(PermissionMappingPatch403.class);
        // 4.0.5
        patches.add(NodeTypeChangePatch.class);
        patches.add(ExpandDescriptionsPatch.class);
        // 4.0.6
        patches.add(TaskOpenedByExecutorsPatch.class);
        // 4.1.0
        patches.add(AddNodeIdToProcessLogPatch.class);
        patches.add(AddSubProcessIndexColumn.class);
        // 4.1.1
        patches.add(AddCreateDateColumns.class);
        // 4.2.0
        patches.add(AddEmbeddedFileForBotTask.class);
        patches.add(AddColumnForEmbeddedBotTaskFileName.class);
        patches.add(AddSettingsTable.class);
        patches.add(AddSequentialFlagToBot.class);
        patches.add(CreateAggregatedLogsTables.class);
        patches.add(TaskCreateLogSeverityChangedPatch.class);
        patches.add(AddColumnsToSubstituteEscalatedTasksPatch.class);
        // 4.2.1
        patches.add(AddMultiTaskIndexToTaskPatch.class);
        // 4.2.2
        patches.add(AddDeploymentAuditPatch.class);
        // 4.3.0
        patches.add(AddAggregatedTaskIndexPatch.class);
        patches.add(AddParentProcessIdPatch.class);
        patches.add(CreateReportsTables.class);
        patches.add(AddDueDateExpressionToJobAndTask.class);
        patches.add(AddBatchPresentationIsSharedPatch.class);
        patches.add(ExpandVarcharPatch.class);
        patches.add(AddProcessAndTokenExecutionStatusPatch.class);
        patches.add(CreateAdminScriptTables.class);
        patches.add(AddVariableUniqueKeyPatch.class);
        patches.add(AddTokenErrorDataPatch.class);
        patches.add(AddTitleAndDepartmentColumnsToActorPatch.class);
        patches.add(AddAssignDateColumnPatch.class);
        patches.add(EmptyPatch.class);
        patches.add(AddTokenMessageSelectorPatch.class);
        patches.add(AddSubprocessBindingDatePatch.class);
        patches.add(AddTransactionalBotSupport.class);
        dbPatches = Collections.unmodifiableList(patches);
    };

    @Autowired
    private ConstantDAO constantDAO;
    @Autowired
    private ExecutorDAO executorDAO;
    @Autowired
    private PermissionDAO permissionDAO;
    @Autowired
    private LocalizationDAO localizationDAO;
    @Autowired
    private TokenDAO tokenDAO;
    @Autowired
    private ProcessDAO processDAO;
    @Autowired
    private IProcessDefinitionLoader processDefinitionLoader;

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        try {
            ApplicationContextFactory.setApplicationContext(event.getApplicationContext());
            log.info("initializing database");
            Integer databaseVersion = constantDAO.getDatabaseVersion();
            if (databaseVersion != null) {
                applyPatches(databaseVersion);
            } else {
                log.info("database is empty, initializing...");
                SchemaExport schemaExport = new SchemaExport(ApplicationContextFactory.getConfiguration());
                schemaExport.execute(true, true, false, true);
                dbTransactionalInitializer.initialize(dbPatches.size());
            }
            permissionDAO.init();
            if (databaseVersion != null) {
                postProcessPatches(databaseVersion);
            }
            String localizedFileName = "localizations." + Locale.getDefault().getLanguage() + ".xml";
            InputStream stream = ClassLoaderUtil.getAsStream(localizedFileName, getClass());
            if (stream == null) {
                stream = ClassLoaderUtil.getAsStreamNotNull("localizations.xml", getClass());
            }
            List<Localization> localizations = LocalizationParser.parseLocalizations(stream);
            stream = ClassLoaderUtil.getAsStream(SystemProperties.RESOURCE_EXTENSION_PREFIX + localizedFileName, getClass());
            if (stream == null) {
                stream = ClassLoaderUtil.getAsStream(SystemProperties.RESOURCE_EXTENSION_PREFIX + "localizations.xml", getClass());
            }
            if (stream != null) {
                localizations.addAll(LocalizationParser.parseLocalizations(stream));
            }
            localizationDAO.saveLocalizations(localizations, false);
            if (DatabaseProperties.isDynamicSettingsEnabled()) {
                PropertyResources.setDatabaseAvailable(true);
            }
            log.info("initialization completed");
        } catch (Exception e) {
            log.error("initialization failed", e);
        }
    }

    /**
     * Apply patches to initialized database.
     */
    private void applyPatches(int databaseVersion) {
        log.info("Database version: " + databaseVersion + ", code version: " + dbPatches.size());
        while (databaseVersion < dbPatches.size()) {
            DBPatch patch = null;
            try {
                patch = ApplicationContextFactory.createAutowiredBean(dbPatches.get(databaseVersion));
                databaseVersion++;
                log.info("Applying patch " + patch + " (" + databaseVersion + ")");
                dbTransactionalInitializer.execute(patch, databaseVersion);
                log.info("Patch " + patch + "(" + databaseVersion + ") is applied to database successfully.");
            } catch (Throwable th) {
                log.error("Can't apply patch " + patch + "(" + databaseVersion + ").", th);
                break;
            }
        }
    }

    private void postProcessPatches(Integer databaseVersion) {
        while (databaseVersion < dbPatches.size()) {
            DBPatch patch = ApplicationContextFactory.createAutowiredBean(dbPatches.get(databaseVersion));
            databaseVersion++;
            if (patch instanceof IDbPatchPostProcessor) {
                log.info("Post-processing patch " + patch + " (" + databaseVersion + ")");
                try {
                    dbTransactionalInitializer.postExecute((IDbPatchPostProcessor) patch);
                    log.info("Patch " + patch.getClass().getName() + "(" + databaseVersion + ") is post-processed successfully.");
                } catch (Throwable th) {
                    log.error("Can't post-process patch " + patch.getClass().getName() + "(" + databaseVersion + ").", th);
                    break;
                }
            }
        }
    }
}

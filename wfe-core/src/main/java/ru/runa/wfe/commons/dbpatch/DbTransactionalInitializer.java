package ru.runa.wfe.commons.dbpatch;

import com.google.common.collect.Lists;
import java.util.List;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import ru.runa.wfe.commons.SystemProperties;
import ru.runa.wfe.commons.dao.ConstantDAO;
import ru.runa.wfe.security.SecuredObjectType;
import ru.runa.wfe.security.dao.PermissionDAO;
import ru.runa.wfe.user.Actor;
import ru.runa.wfe.user.Executor;
import ru.runa.wfe.user.Group;
import ru.runa.wfe.user.SystemExecutors;
import ru.runa.wfe.user.dao.ExecutorDAO;

@Transactional
public class DbTransactionalInitializer {
    protected static final Log log = LogFactory.getLog(DbTransactionalInitializer.class);
    @Autowired
    private ConstantDAO constantDAO;
    @Autowired
    private ExecutorDAO executorDAO;
    @Autowired
    private PermissionDAO permissionDAO;

    public void execute(DBPatch dbPatch, int databaseVersion) throws Exception {
        dbPatch.execute();
        constantDAO.setDatabaseVersion(databaseVersion);
    }

    public void postExecute(IDbPatchPostProcessor dbPatch) throws Exception {
        dbPatch.postExecute();
    }

    /**
     * Initialize empty database
     */
    public void initialize(int version) {
        try {
            insertInitialData();
            constantDAO.setDatabaseVersion(version);
        } catch (Throwable th) {
            log.info("unable to insert initial data", th);
        }
    }

    /**
     * Inserts initial data on database creation stage
     */
    private void insertInitialData() {
        // create privileged Executors
        String administratorName = SystemProperties.getAdministratorName();
        Actor admin = new Actor(administratorName, administratorName, administratorName);
        admin = executorDAO.create(admin);
        executorDAO.setPassword(admin, SystemProperties.getAdministratorDefaultPassword());
        String administratorsGroupName = SystemProperties.getAdministratorsGroupName();
        Group adminGroup = executorDAO.create(new Group(administratorsGroupName, administratorsGroupName));
        executorDAO.create(new Group(SystemProperties.getBotsGroupName(), SystemProperties.getBotsGroupName()));
        List<? extends Executor> adminWithGroupExecutors = Lists.newArrayList(adminGroup, admin);
        executorDAO.addExecutorToGroup(admin, adminGroup);
        executorDAO.create(new Actor(SystemExecutors.PROCESS_STARTER_NAME, SystemExecutors.PROCESS_STARTER_DESCRIPTION));
        // define executor permissions
        permissionDAO.addType(SecuredObjectType.ACTOR, adminWithGroupExecutors);
        permissionDAO.addType(SecuredObjectType.GROUP, adminWithGroupExecutors);
        // define system permissions
        permissionDAO.addType(SecuredObjectType.SYSTEM, adminWithGroupExecutors);
        permissionDAO.addType(SecuredObjectType.RELATIONGROUP, adminWithGroupExecutors);
        permissionDAO.addType(SecuredObjectType.RELATION, adminWithGroupExecutors);
        permissionDAO.addType(SecuredObjectType.RELATIONPAIR, adminWithGroupExecutors);
        permissionDAO.addType(SecuredObjectType.BOTSTATION, adminWithGroupExecutors);
        permissionDAO.addType(SecuredObjectType.DEFINITION, adminWithGroupExecutors);
        permissionDAO.addType(SecuredObjectType.PROCESS, adminWithGroupExecutors);
        permissionDAO.addType(SecuredObjectType.REPORT, adminWithGroupExecutors);
    }

}
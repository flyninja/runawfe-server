package ru.runa.wfe.service.impl;

import com.google.common.base.Objects;
import com.google.common.base.Throwables;
import javax.annotation.Resource;
import javax.ejb.ActivationConfigProperty;
import javax.ejb.MessageDriven;
import javax.ejb.MessageDrivenContext;
import javax.ejb.TransactionManagement;
import javax.ejb.TransactionManagementType;
import javax.interceptor.Interceptors;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.ObjectMessage;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ejb.interceptor.SpringBeanAutowiringInterceptor;
import ru.runa.wfe.InternalApplicationException;
import ru.runa.wfe.audit.dao.ProcessLogDao;
import ru.runa.wfe.commons.TransactionListener;
import ru.runa.wfe.commons.TransactionListeners;
import ru.runa.wfe.commons.Utils;
import ru.runa.wfe.definition.dao.ProcessDefinitionLoader;
import ru.runa.wfe.execution.ExecutionContext;
import ru.runa.wfe.execution.Token;
import ru.runa.wfe.execution.dao.TokenDao;
import ru.runa.wfe.lang.Node;
import ru.runa.wfe.lang.ProcessDefinition;
import ru.runa.wfe.service.interceptors.EjbExceptionSupport;
import ru.runa.wfe.service.interceptors.PerformanceObserver;

/**
 * @since 4.3.0
 * @author Alex Chernyshev
 */
@MessageDriven(activationConfig = { @ActivationConfigProperty(propertyName = "destination", propertyValue = "queue/nodeAsyncExecution"),
        @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "javax.jms.Queue") })
@TransactionManagement(TransactionManagementType.CONTAINER)
@Interceptors({ EjbExceptionSupport.class, PerformanceObserver.class, SpringBeanAutowiringInterceptor.class })
public class NodeAsyncExecutionBean implements MessageListener {
    private static final Log log = LogFactory.getLog(NodeAsyncExecutionBean.class);
    @Autowired
    private TokenDao tokenDao;
    @Autowired
    private ProcessDefinitionLoader processDefinitionLoader;
    @Autowired
    private ProcessLogDao processLogDao;
    @Resource
    private MessageDrivenContext context;

    @Override
    public void onMessage(Message jmsMessage) {
        Long processId;
        Long tokenId;
        String nodeId;
        try {
            ObjectMessage message = (ObjectMessage) jmsMessage;
            log.debug("On message " + message.getJMSMessageID());
            processId = message.getLongProperty("processId");
            tokenId = message.getLongProperty("tokenId");
            nodeId = message.getStringProperty("nodeId");
        } catch (JMSException e) {
            throw Throwables.propagate(e);
        }
        if (!ProcessTokenSynchronization.lock(processId, tokenId)) {
            context.setRollbackOnly();
            return;
        }
        try {
            handleMessage(processId, tokenId, nodeId);
            for (TransactionListener listener : TransactionListeners.get()) {
                try {
                    // TODO transaction in progress, so timeout must be long enough
                    listener.onTransactionComplete(null);
                } catch (Throwable th) {
                    log.error(th);
                }
            }
        } catch (Exception e) {
            log.error(jmsMessage, e);
            context.setRollbackOnly();
        } finally {
            ProcessTokenSynchronization.unlock(processId, tokenId);
        }
    }

    private void handleMessage(Long processId, Long tokenId, String nodeId) throws JMSException {
        log.debug("Handling token execution request: {processId=" + processId + ", tokenId=" + tokenId + ", nodeId=" + nodeId + "}");
        Token token = tokenDao.getNotNull(tokenId);
        if (token.getProcess().hasEnded()) {
            log.debug("Ignored token execution request for ended " + token.getProcess());
            return;
        }
        if (!Objects.equal(nodeId, token.getNodeId())) {
            throw new InternalApplicationException(token + " expected to be in node " + nodeId);
        }
        ProcessDefinition processDefinition = processDefinitionLoader.getDefinition(token.getProcess());
        Node node = processDefinition.getNodeNotNull(token.getNodeId());
        try {
            ExecutionContext executionContext = new ExecutionContext(processDefinition, token);
            node.handle(executionContext);
        } catch (Throwable th) {
            log.error(processId + ":" + tokenId, th);
            Utils.sendNodeAsyncFailedExecutionMessage(tokenId, th);
        }
    }
}
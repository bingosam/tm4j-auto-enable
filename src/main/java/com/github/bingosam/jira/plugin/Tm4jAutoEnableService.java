package com.github.bingosam.jira.plugin;

import com.atlassian.event.api.EventListener;
import com.atlassian.event.api.EventPublisher;
import com.atlassian.jira.event.ProjectCreatedEvent;
import com.atlassian.plugin.spring.scanner.annotation.imports.JiraImport;
import org.apache.commons.beanutils.MethodUtils;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.lang.reflect.InvocationTargetException;

/**
 * <p>Title: Module Information  </p>
 * <p>Description: Function Description  </p>
 * <p>Copyright: Copyright (c) 2021     </p>
 * <p>Create Time: 2021/7/13          </p>
 *
 * @author zhang kunbin
 */
@Component
public class Tm4jAutoEnableService
        implements InitializingBean, DisposableBean {

    private static final Logger log = LoggerFactory.getLogger(Tm4jAutoEnableService.class);

    private static final String TM4J_PROJECT_SERVICE = "com.kanoah.testmanager.service.publicservice.TM4JProjectPublicService";

    @JiraImport
    private final EventPublisher eventPublisher;

    private final BundleContext bundleContext;

    @Autowired
    public Tm4jAutoEnableService(final EventPublisher eventPublisher
            , final BundleContext bundleContext
    ) {
        this.eventPublisher = eventPublisher;
        this.bundleContext = bundleContext;
    }

    @EventListener
    public void enableTm4jAfterProjectCreated(ProjectCreatedEvent event)
            throws NoSuchMethodException
            , IllegalAccessException
            , InvocationTargetException {
        long timestamp = System.currentTimeMillis();
        ServiceReference reference = bundleContext.getServiceReference(TM4J_PROJECT_SERVICE);
        if (reference == null) {
            log.error("tm4j is neither enabled nor installed, unable to enable tm4j automatically!");
            return;
        }

        try {
            Object tm4jProjectPublicService = bundleContext.getService(reference);
            if (null == tm4jProjectPublicService) {
                log.error("Unable to get the service named " + TM4J_PROJECT_SERVICE);
                return;
            }

            MethodUtils.invokeMethod(tm4jProjectPublicService, "toggleEnableProject",
                    new Object[]{
                            event.getId().intValue(), true
                    });
        } finally {
            bundleContext.ungetService(reference);
        }
        log.info("tm4j enabled after " + (System.currentTimeMillis() - timestamp) + " milliseconds.");
    }

    @Override
    public void destroy() {
        log.info("Disable Tm4jAutoEnableService.");
        eventPublisher.unregister(this);
    }

    @Override
    public void afterPropertiesSet() {
        log.info("Enable Tm4jAutoEnableService.");
        eventPublisher.register(this);
    }
}

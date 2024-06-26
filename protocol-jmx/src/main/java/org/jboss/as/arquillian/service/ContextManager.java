/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.arquillian.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.SetupAction;
import org.jboss.modules.Module;
import org.wildfly.security.manager.WildFlySecurityManager;

/**
 * Sets up and tears down a set of contexts, represented by a list of {@link SetupAction}s. If {@link #setup(java.util.Map)}
 * completes
 * successfully then {@link #teardown(java.util.Map)} must be called.
 *
 * @author Stuart Douglas
 * @author <a href="mailto:alr@jboss.org">Andrew Lee Rubinger</a>
 *
 */
public class ContextManager {

    private final List<SetupAction> setupActions;
    private final ArquillianConfig config;

    ContextManager(final ArquillianConfig config, final List<SetupAction> setupActions) {
        this.config = config;
        final List<SetupAction> actions = new ArrayList<SetupAction>(setupActions);
        Collections.sort(actions, new Comparator<SetupAction>() {

            @Override
            public int compare(final SetupAction arg0, final SetupAction arg1) {
                return arg0.priority() > arg1.priority() ? -1 : arg0.priority() == arg1.priority() ? 0 : 1;
            }
        });
        this.setupActions = Collections.unmodifiableList(actions);
    }

    /**
     * Sets up the contexts. If any of the setup actions fail then any setup contexts are torn down, and then the exception is
     * wrapped and thrown
     */
    public void setup(final Map<String, Object> properties) {
        final List<SetupAction> successfulActions = new ArrayList<SetupAction>();
        final DeploymentUnit depUnit = config.getDeploymentUnit();
        final Module module = depUnit.getAttachment(Attachments.MODULE);
        ClassLoader tccl = WildFlySecurityManager.setCurrentContextClassLoaderPrivileged(module.getClassLoader());
        try {
            for (final SetupAction action : setupActions) {
                try {
                    action.setup(properties);
                    successfulActions.add(action);
                } catch (final Throwable e) {
                    for (SetupAction s : successfulActions) {
                        try {
                            s.teardown(properties);
                        } catch (final Throwable t) {
                            // we ignore these, and just propagate the exception that caused the setup to fail
                        }
                    }
                    throw new RuntimeException(e);
                }
            }
        } finally {
            WildFlySecurityManager.setCurrentContextClassLoaderPrivileged(tccl);
        }
    }

    /**
     * Tears down the contexts. If an exception is thrown by a {@link SetupAction} it is wrapped and re-thrown after all
     * {@link SetupAction#teardown(java.util.Map)} methods have been called.
     * <p>
     * Contexts are torn down in the opposite order to which they are set up (i.e. the first context set up is the last to be
     * torn down).
     * <p>
     * If more than one teardown() method thrown an exception then only the first is propagated.
     */
    public void teardown(final Map<String, Object> properties) {
        Throwable exceptionToThrow = null;
        final ListIterator<SetupAction> itr = setupActions.listIterator(setupActions.size());
        final DeploymentUnit depUnit = config.getDeploymentUnit();
        final Module module = depUnit.getAttachment(Attachments.MODULE);
        ClassLoader tccl = WildFlySecurityManager.setCurrentContextClassLoaderPrivileged(module.getClassLoader());
        try {
            while (itr.hasPrevious()) {
                final SetupAction action = itr.previous();
                try {
                    action.teardown(properties);
                } catch (Throwable e) {
                    if (exceptionToThrow == null) {
                        exceptionToThrow = e;
                    }
                }
            }
            if (exceptionToThrow != null) {
                throw new RuntimeException(exceptionToThrow);
            }
        } finally {
            WildFlySecurityManager.setCurrentContextClassLoaderPrivileged(tccl);
        }
    }
}

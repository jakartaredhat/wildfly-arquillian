/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.arquillian.domain.container.controller;

import org.jboss.arquillian.container.spi.event.SetupContainers;
import org.jboss.arquillian.core.api.Injector;
import org.jboss.arquillian.core.api.Instance;
import org.jboss.arquillian.core.api.InstanceProducer;
import org.jboss.arquillian.core.api.annotation.ApplicationScoped;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.core.api.annotation.Observes;
import org.wildfly.arquillian.domain.api.DomainContainerController;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
public class ClientDomainContainerControllerCreator {

    @Inject
    @ApplicationScoped
    private InstanceProducer<DomainContainerController> controller;

    @Inject
    private Instance<Injector> injector;

    @SuppressWarnings("UnusedParameters")
    public void create(@Observes SetupContainers event) {
        controller.set(injector.get().inject(new ClientDomainContainerController()));
    }
}

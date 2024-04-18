/*
 * JBoss, Home of Professional Open Source.
 *
 * Copyright 2024 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
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

package org.jboss.as.arquillian.container;

import java.lang.annotation.Annotation;

import org.jboss.arquillian.core.api.Instance;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.wildfly.plugin.tools.server.ServerManager;

/**
 * A provider for {@link ArquillianResource} injection of a {@link ServerManager}.
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
public class ServerManagerProvider extends AbstractTargetsContainerProvider {

    @Inject
    private Instance<ServerManager> serverManager;

    @Override
    public boolean canProvide(final Class<?> type) {
        return ServerManager.class.isAssignableFrom(type);
    }

    @Override
    public Object doLookup(final ArquillianResource resource, final Annotation... qualifiers) {
        return serverManager.get();
    }
}

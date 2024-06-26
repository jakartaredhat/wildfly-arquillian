/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.arquillian.service;

import static org.jboss.as.arquillian.container.Authentication.getCallbackHandler;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.lang.annotation.Annotation;
import java.net.URI;
import java.net.URL;

import org.jboss.arquillian.core.api.annotation.Observes;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.arquillian.test.spi.enricher.resource.ResourceProvider;
import org.jboss.arquillian.test.spi.event.suite.AfterSuite;
import org.jboss.as.arquillian.container.Authentication;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.client.ModelControllerClientConfiguration;

/**
 * resource provider that allows the ManagementClient to be injected inside the container.
 */
public class InContainerManagementClientProvider implements ResourceProvider {

    private static ManagementClient current;

    /**
     * {@inheritDoc}
     *
     * @see org.jboss.arquillian.test.spi.enricher.resource.ResourceProvider#canProvide(Class)
     */
    @Override
    public boolean canProvide(final Class<?> type) {
        return type.isAssignableFrom(ManagementClient.class);
    }

    @Override
    public synchronized Object lookup(final ArquillianResource arquillianResource, final Annotation... annotations) {
        if (current != null) {
            return current;
        }
        final URL resourceUrl = getClass().getClassLoader().getResource("META-INF/org.jboss.as.managementConnectionProps");
        if (resourceUrl != null) {
            InputStream in = null;
            String managementPort;
            String address;
            String protocol;
            try {
                in = resourceUrl.openStream();
                ObjectInputStream inputStream = new ObjectInputStream(in);
                managementPort = (String) inputStream.readObject();
                address = (String) inputStream.readObject();
                protocol = (String) inputStream.readObject();
                String authenticationConfig = (String) inputStream.readObject();
                if (address == null) {
                    address = "localhost";
                }
                if (managementPort == null) {
                    managementPort = "9990";
                }
                if (protocol == null) {
                    protocol = "remote+http";
                }
                final int port = Integer.parseInt(managementPort);

                // Configure a client for in-container tests based on the serialized data within the deployment
                final ModelControllerClientConfiguration.Builder builder = new ModelControllerClientConfiguration.Builder()
                        .setHostName(address)
                        .setPort(port)
                        .setProtocol(protocol);

                if (Authentication.username != null && !Authentication.username.isEmpty()) {
                    builder.setHandler(getCallbackHandler());
                }

                if (authenticationConfig != null) {
                    builder.setAuthenticationConfigUri(URI.create(authenticationConfig));
                }
                current = new ManagementClient(ModelControllerClient.Factory.create(builder.build()), address, port, protocol);

            } catch (IOException e) {
                throw new RuntimeException(e);
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            } finally {
                if (in != null) {
                    try {
                        in.close();
                    } catch (IOException e) {
                        // ignore
                    }
                }
            }

        }
        return current;
    }

    public synchronized void cleanUp(@Observes AfterSuite afterSuite) {
        if (current != null) {
            current.close();
            current = null;
        }
    }
}

/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.arquillian.setup;

import java.util.Map;

import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.client.helpers.Operations;
import org.jboss.as.controller.client.helpers.Operations.CompositeOperationBuilder;
import org.jboss.dmr.ModelNode;

/**
 * A setup task for Arquillian tests which set system properties in WildFly and remove them when the test is complete.
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
public abstract class SystemPropertyServerSetupTask implements ServerSetupTask {

    private final Map<String, String> properties;

    /**
     * Creates a new setup task which defines the provided properties.
     *
     * @param properties the properties to add
     */
    protected SystemPropertyServerSetupTask(final Map<String, String> properties) {
        this.properties = properties;
    }

    @Override
    public void setup(final ManagementClient managementClient, final String containerId) throws Exception {
        final CompositeOperationBuilder builder = CompositeOperationBuilder.create();
        for (Map.Entry<String, String> entry : properties.entrySet()) {
            final ModelNode address = Operations.createAddress("system-property", entry.getKey());
            final ModelNode op = Operations.createAddOperation(address);
            op.get("value").set(entry.getValue());
            builder.addStep(op);
        }
        executeOperation(managementClient, builder.build(),
                (result) -> String.format("Failed to add system properties %s%n%s", properties,
                        Operations.getFailureDescription(result)
                                .asString()));
    }

    @Override
    public void tearDown(final ManagementClient managementClient, final String containerId) throws Exception {
        final CompositeOperationBuilder builder = CompositeOperationBuilder.create();
        for (Map.Entry<String, String> entry : properties.entrySet()) {
            final ModelNode address = Operations.createAddress("system-property", entry.getKey());
            builder.addStep(Operations.createRemoveOperation(address));
        }
        executeOperation(managementClient, builder.build(),
                (result) -> String.format("Failed to remove system properties %s%n%s", properties,
                        Operations.getFailureDescription(result)
                                .asString()));
    }
}

/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2016, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

/**
 *
 */
package org.jboss.as.controller;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ATTRIBUTES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.INCLUDE_RUNTIME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATIONS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OUTCOME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_RESOURCE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESULT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUCCESS;
import static org.jboss.dmr.ModelType.INT;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.descriptions.NonResolvingResourceDescriptionResolver;
import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.controller.operations.global.GlobalNotifications;
import org.jboss.as.controller.operations.global.GlobalOperationHandlers;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

/**
 * Tests cases for registration.
 *
 * Depending on the server environment (process type and running mode), attributes and operations
 * may not be actually registered on the MMR.
 */
public class RegistrationTestCase {

    private static final PathElement ELEMENT = PathElement.pathElement("testing", "resource");
    private static final PathAddress ADDRESS = PathAddress.pathAddress(ELEMENT);
    private static final String TEST_METRIC = "test-metric";
    private static final String FORCED_TEST_METRIC = "forced-test-metric";
    private static final String TEST_RUNTIME_ATTRIBUTE = "test-runtime-attribute";
    private static final String FORCED_TEST_RUNTIME_ATTRIBUTE = "forced-test-runtime-attribute";
    private static final String TEST_OPERATION = "test-operation";
    private static final String FORCED_TEST_OPERATION = "forced-test-operation";

    private static final Executor executor = Executors.newCachedThreadPool();

    private ServiceContainer container;
    private ModelController controller;
    private ModelControllerClient client;

    // Metric

    @Test
    public void registerMetricOnEmbeddedServerRegistersIt() throws Exception {
        checkMetricRegistration(ProcessType.EMBEDDED_SERVER, true);
    }

    @Test
    public void registerMetricOnStandaloneServerRegistersIt() throws Exception {
        checkMetricRegistration(ProcessType.STANDALONE_SERVER, true);
    }

    @Test
    public void registerMetricOnDomainServerRegistersIt() throws Exception {
        checkMetricRegistration(ProcessType.DOMAIN_SERVER, true);
    }

    @Test
    public void registerMetricOnHostControllerDoesNotRegisterIt() throws Exception {
        checkMetricRegistration(ProcessType.HOST_CONTROLLER, false);
    }

    @Test
    public void registerMetricOnEmbeddedHostControllerDoesNotRegisterIt() throws Exception {
        checkMetricRegistration(ProcessType.EMBEDDED_HOST_CONTROLLER, false);
    }

    // Runtime attribute

    @Test
    public void registerRuntimeAttributeOnEmbeddedServerRegistersItIt() throws Exception {
        checkRuntimeAttributeRegistration(ProcessType.EMBEDDED_SERVER, true);
    }

    @Test
    public void registerRuntimeAttributeOnStandaloneServerRegistersIt() throws Exception {
        checkRuntimeAttributeRegistration(ProcessType.STANDALONE_SERVER, true);
    }

    @Test
    public void registerRuntimeAttributeOnDomainServerRegistersIt() throws Exception {
        checkRuntimeAttributeRegistration(ProcessType.DOMAIN_SERVER, true);
    }

    @Test
    public void registeRuntimeAttributeOnHostControllerDoesNotRegisterIt() throws Exception {
        checkRuntimeAttributeRegistration(ProcessType.HOST_CONTROLLER, false);
    }

    @Test
    public void registerRuntimeAttributeOnEmbeddedHostControllerDoesNotRegisterIt() throws Exception {
        checkRuntimeAttributeRegistration(ProcessType.EMBEDDED_HOST_CONTROLLER, false);
    }

    // Runtime operation

    @Test
    public void registerRuntimeOperationOnEmbeddedServerRegistersIt() throws Exception {
        checkOperationRegistration(ProcessType.EMBEDDED_SERVER, true);
    }

    @Test
    public void registerRuntimeOperationOnStandaloneServerRegistersIt() throws Exception {
        checkOperationRegistration(ProcessType.STANDALONE_SERVER, true);
    }

    @Test
    public void registerRuntimeOperationOnDomainServerRegistersIt() throws Exception {
        checkOperationRegistration(ProcessType.DOMAIN_SERVER, true);
    }

    @Test
    public void registerRuntimeOperationOnHostControllerDoesNotRegisterIt() throws Exception {
        checkOperationRegistration(ProcessType.HOST_CONTROLLER, false);
    }

    @Test
    public void registerRuntimeOperationOnEmbeddedHostControllerDoesNotRegisterIt() throws Exception {
        checkOperationRegistration(ProcessType.EMBEDDED_HOST_CONTROLLER, false);
    }

    /**
     * Check whether the test-operation is registered. It is the case, it must be described in read-resource-description
     * and it can be executed.
     * Check also that the forced-test-operation is always be registered and can be executed.
     */
    private void checkOperationRegistration(ProcessType processType, boolean operationIsRegistered) throws Exception {
        setupController(processType, new TestResourceDefinition());
        ModelNode rrd = Util.getReadResourceDescriptionOperation(ADDRESS);
        rrd.get(OPERATIONS).set(true);
        ModelNode description = getResult(client.execute(rrd));
        assertEquals(description.toJSONString(false), operationIsRegistered, description.hasDefined(OPERATIONS, TEST_OPERATION));
        if (operationIsRegistered) {
            executeOperation(TEST_OPERATION, 1000);
        }
        // forced operation is always registered
        assertTrue(description.hasDefined(OPERATIONS, FORCED_TEST_OPERATION));
        executeOperation(FORCED_TEST_OPERATION, 2000);
    }

    /**
     * Check whether the test-metric is registered. It is the case, it must be described in read-resource-description
     * and it can be read.
     * Check also that the forced-test-metric is always be registered and can be read.
     */
    private void checkMetricRegistration(ProcessType processType, boolean metricIsRegistered) throws Exception {
        setupController(processType, new TestResourceDefinition());
        ModelNode description = getResult(client.execute(Util.getReadResourceDescriptionOperation(ADDRESS)));
        assertEquals(description.toJSONString(false), metricIsRegistered, description.hasDefined(ATTRIBUTES, TEST_METRIC));
        if (metricIsRegistered) {
            readAttribute(TEST_METRIC, 1000);
        }
        // forced test metric is always registered
        assertTrue(description.hasDefined(ATTRIBUTES, FORCED_TEST_METRIC));
        readAttribute(FORCED_TEST_METRIC, 2000);
    }

    /**
     * Check whether the test-metric is registered. It is the case, it must be described in read-resource-description
     * and it can be read.
     * Check also that the forced-test-metric is always be registered and can be read.
     */
    private void checkRuntimeAttributeRegistration(ProcessType processType, boolean attributeIsRegistered) throws Exception {
        setupController(processType, new TestResourceDefinition());
        ModelNode rrd = Util.getReadResourceDescriptionOperation(ADDRESS);
        rrd.get(OPERATIONS).set(true);
        ModelNode description = getResult(client.execute(rrd));
        assertEquals(description.toJSONString(false), attributeIsRegistered, description.hasDefined(ATTRIBUTES, TEST_RUNTIME_ATTRIBUTE));
        if (attributeIsRegistered) {
            readAttribute(TEST_RUNTIME_ATTRIBUTE, 3000);
        }
        // forced test metric is always registered
        assertTrue(description.hasDefined(ATTRIBUTES, FORCED_TEST_RUNTIME_ATTRIBUTE));
        readAttribute(FORCED_TEST_RUNTIME_ATTRIBUTE, 4000);
    }

    private void executeOperation(String operationName, int expectedValue) throws Exception {
        ModelNode op = Util.getEmptyOperation(operationName, ADDRESS.toModelNode());
        ModelNode result = getResult(client.execute(op));
        assertEquals(expectedValue, result.asInt());
    }

    private void readAttribute(String attributeName, int expectedValue) throws Exception {
        ModelNode result = getResult(client.execute(Util.getReadAttributeOperation(ADDRESS, attributeName)));
        assertEquals(expectedValue, result.asInt());

        ModelNode rr = Util.createEmptyOperation(READ_RESOURCE_OPERATION, ADDRESS);
        rr.get(INCLUDE_RUNTIME).set(true);
        result = getResult(client.execute(rr));
        Assert.assertTrue(result.hasDefined(attributeName));
        assertEquals(expectedValue, result.get(attributeName).asInt());
    }

    private ModelNode getResult(ModelNode result) {
        assertEquals(SUCCESS, result.get(OUTCOME).asString());
        return result.get(RESULT);
    }


    @After
    public void shutdownServiceContainer() {

        if (client != null) {
            try {
                client.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if (container != null) {
            container.shutdown();
            try {
                container.awaitTermination(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                container = null;
            }
        }
    }

    private ManagementResourceRegistration setupController(ProcessType processType, TestResourceDefinition resourceDefinition) throws InterruptedException {

        container = ServiceContainer.Factory.create(TEST_OPERATION);
        ServiceTarget target = container.subTarget();
        ModelControllerService svc = new ModelControllerService(processType, resourceDefinition);
        ServiceBuilder<ModelController> builder = target.addService(ServiceName.of("ModelController"), svc);
        builder.install();
        svc.awaitStartup(30, TimeUnit.SECONDS);
        controller = svc.getValue();
        ModelNode setup = Util.getEmptyOperation("setup", new ModelNode());
        controller.execute(setup, null, null, null);

        client = controller.createClient(executor);

        return svc.managementControllerResource;
    }

    private static class ModelControllerService extends TestModelControllerService {

        private final TestResourceDefinition resourceDefinition;
        public ManagementResourceRegistration managementControllerResource;

        public ModelControllerService(ProcessType processType, TestResourceDefinition resourceDefinition) {
            super(processType);
            this.resourceDefinition = resourceDefinition;
        }

        @Override
        protected void initModel(ManagementModel managementModel, Resource modelControllerResource) {
            ManagementResourceRegistration rootRegistration = managementModel.getRootResourceRegistration();
            GlobalOperationHandlers.registerGlobalOperations(rootRegistration, processType);
            GlobalNotifications.registerGlobalNotifications(rootRegistration, processType);
            managementControllerResource = rootRegistration.registerSubModel(resourceDefinition);

            Resource rootResource = managementModel.getRootResource();
            rootResource.registerChild(resourceDefinition.getPathElement(), Resource.Factory.create());
        }
    }


    private static class TestResourceDefinition extends SimpleResourceDefinition {

        // runtime attribute is registered depending on the type of server for the MMR
        private static final SimpleAttributeDefinition RUNTIME_ATTRIBUTE = new SimpleAttributeDefinitionBuilder(TEST_RUNTIME_ATTRIBUTE, ModelType.INT)
                .setDefaultValue(new ModelNode(3000))
                .setStorageRuntime()
                .build();

        // metric is always registered
        private static final SimpleAttributeDefinition FORCED_RUNTIME_ATTRIBUTE = new SimpleAttributeDefinitionBuilder(FORCED_TEST_RUNTIME_ATTRIBUTE, ModelType.INT)
                .setDefaultValue(new ModelNode(4000))
                .setStorageRuntime()
                .forceRegistration()
                .build();

        // metric is registered depending on the type of server for the MMR
        private static final SimpleAttributeDefinition METRIC = new SimpleAttributeDefinitionBuilder(TEST_METRIC, ModelType.INT)
                .setStorageRuntime()
                .setUndefinedMetricValue(new ModelNode(0))
                .build();
        // metric is always registered
        private static final SimpleAttributeDefinition FORCED_METRIC = new SimpleAttributeDefinitionBuilder(FORCED_TEST_METRIC, ModelType.INT)
                .forceRegistration()
                .setUndefinedMetricValue(new ModelNode(0))
                .build();

        private static final ResourceDescriptionResolver RESOLVER = new NonResolvingResourceDescriptionResolver();

        // runtime operation is registered depending on the type of server for the MMR
        private static final OperationDefinition TEST_OP = new SimpleOperationDefinitionBuilder(TEST_OPERATION, RESOLVER)
                .setReplyType(INT)
                .setRuntimeOnly()
                .build();

        // force runtime operation is always registered
        private static final OperationDefinition FORCED_TEST_OP = new SimpleOperationDefinitionBuilder(FORCED_TEST_OPERATION, RESOLVER)
                .setReplyType(INT)
                .setRuntimeOnly()
                .forceRegistration()
                .build();

        public TestResourceDefinition() {
            super(ELEMENT, new NonResolvingResourceDescriptionResolver(),
                    new ModelOnlyAddStepHandler(), new ModelOnlyRemoveStepHandler());
        }


        @Override
        public void registerOperations(ManagementResourceRegistration resourceRegistration) {
            resourceRegistration.registerOperationHandler(TEST_OP, (context, operation) -> context.getResult().set(1000));
            resourceRegistration.registerOperationHandler(FORCED_TEST_OP, (context, operation) -> context.getResult().set(2000));
        }

        @Override
        public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
            resourceRegistration.registerReadOnlyAttribute(RUNTIME_ATTRIBUTE, null);
            resourceRegistration.registerReadOnlyAttribute(FORCED_RUNTIME_ATTRIBUTE, null);
            resourceRegistration.registerMetric(METRIC, (context, operation) -> context.getResult().set(1000));
            resourceRegistration.registerMetric(FORCED_METRIC, (context, operation) -> context.getResult().set(2000));
        }

    }
}

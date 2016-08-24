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

package org.jboss.as.test.integration.domain.events;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.test.integration.domain.events.provider.TestListener;
import org.jboss.as.test.integration.domain.management.util.DomainLifecycleUtil;
import org.jboss.as.test.integration.domain.management.util.DomainTestSupport;
import org.jboss.as.test.integration.domain.management.util.DomainTestUtils;
import org.jboss.as.test.integration.management.util.MgmtOperationException;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.exporter.StreamExporter;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2016 Red Hat inc.
 */
public class ProcessStateListenerTestCase {


    private static DomainTestSupport testSupport;

    public static void initializeModule(final DomainTestSupport support) throws IOException {
        // Get module.xml, create modules.jar and add to test config
        final InputStream moduleXml = getModuleXml("process-state-listener-module.xml");
        StreamExporter exporter = createResourceRoot(TestListener.class.getPackage());
        Map<String, StreamExporter> content = Collections.singletonMap("process-state-listener.jar", exporter);
        support.addTestModule("org.foo", moduleXml, content);
    }

    static InputStream getModuleXml(final String name) {
        // Get the module xml
        final ClassLoader tccl = Thread.currentThread().getContextClassLoader();
        return tccl.getResourceAsStream("extension/" + name);
    }

    static StreamExporter createResourceRoot(Package... additionalPackages) throws IOException {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class);
        if (additionalPackages != null) {
            for (Package pkg : additionalPackages) {
                archive.addPackage(pkg);
            }
        }
        return archive.as(ZipExporter.class);
    }

    private static ModelNode executeForResult(final ModelNode op, final ModelControllerClient modelControllerClient) throws IOException, MgmtOperationException {
        try {
            return DomainTestUtils.executeForResult(op, modelControllerClient);
        } catch (MgmtOperationException e) {
            System.out.println(" Op failed:");
            System.out.println(e.getOperation());
            System.out.println("with result");
            System.out.println(e.getResult());
            throw e;
        }
    }

    @BeforeClass
    public static void setupDomain() throws Exception {
        testSupport = DomainTestSupport.create(DomainTestSupport.Configuration.create(ProcessStateListenerTestCase.class.getSimpleName(),
                "domain-configs/domain-minimal.xml", "host-configs/host-master.xml", "host-configs/host-minimal.xml"));
        initializeModule(testSupport);
    }

    @Test
    public void testListener() throws Throwable {
        testSupport.start();

        File tempFile = File.createTempFile("events", ".txt");

        DomainLifecycleUtil domainMasterLifecycleUtil = testSupport.getDomainMasterLifecycleUtil();
        PathAddress address = domainMasterLifecycleUtil.getAddress()
                .append("core-service", "management")
                .append("service", "process-state-listeners");
        ModelNode listeners = new ModelNode();
        ModelNode listener = new ModelNode();
        listener.get("class").set(TestListener.class.getName());
        listener.get("module").set("org.foo");
        ModelNode props = new ModelNode();
        props.add("file", tempFile.getAbsolutePath());
        listener.get("properties").set(props);
        listeners.add(listener);

        ModelNode addListener = Util.createAddOperation(address);
        addListener.get("process-state-listeners").set(listeners);
        executeForResult(addListener, domainMasterLifecycleUtil.getDomainClient());

        ModelNode removeJmxSubsystemListener = Util.createRemoveOperation(domainMasterLifecycleUtil.getAddress()
                .append("subsystem", "jmx"));
        executeForResult(removeJmxSubsystemListener, domainMasterLifecycleUtil.getDomainClient());

        reload(testSupport);

        ModelNode shutdown = Util.createEmptyOperation("shutdown", domainMasterLifecycleUtil.getAddress());
        executeForResult(shutdown, domainMasterLifecycleUtil.getDomainClient());

        testSupport.stop();

        List<String> output = Files.readAllLines(tempFile.toPath());

        assertEquals(output.toString(), 2, output.size());
        assertEquals("HOST_CONTROLLER NORMAL running reload-required", output.get(0));
        assertEquals("HOST_CONTROLLER NORMAL reload-required stopping", output.get(1));

        tempFile.delete();

        fail("wth");
    }

    private void reload(DomainTestSupport testSupport) throws Exception {
        ModelNode reload = Util.createEmptyOperation("reload", testSupport.getDomainMasterLifecycleUtil().getAddress());
        testSupport.getDomainMasterLifecycleUtil().executeAwaitConnectionClosed(reload);
        testSupport.getDomainMasterLifecycleUtil().connect();
        testSupport.getDomainMasterLifecycleUtil().awaitHostController(System.currentTimeMillis());
    }
}

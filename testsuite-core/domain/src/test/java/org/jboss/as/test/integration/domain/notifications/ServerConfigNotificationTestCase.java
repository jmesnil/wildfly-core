/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.integration.domain.notifications;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.WRITE_ATTRIBUTE_OPERATION;
import static org.jboss.as.test.integration.domain.management.util.DomainTestUtils.waitUntilState;
import static org.junit.Assert.fail;

import java.io.IOException;

import org.jboss.as.controller.client.helpers.domain.DomainClient;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.test.integration.domain.management.util.DomainLifecycleUtil;
import org.jboss.as.test.integration.domain.management.util.DomainTestSupport;
import org.jboss.as.test.integration.domain.suites.DomainTestSuite;
import org.jboss.as.test.integration.management.util.MgmtOperationException;
import org.jboss.dmr.ModelNode;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2014 Red Hat inc.
 */
public class ServerConfigNotificationTestCase {

    private static DomainTestSupport domainTestSupport;

    private static final ModelNode mainOneConfigAddress = new ModelNode();

    static {
        // (host=slave),(server-config=main-one)
        mainOneConfigAddress.add("host", "master");
        mainOneConfigAddress.add("server-config", "main-one");
    }

    @BeforeClass
    public static void setupDomain() {
        domainTestSupport = DomainTestSuite.createSupport(ServerConfigNotificationTestCase.class.getName());
    }

    @AfterClass
    public static void tearDownDomain() {
        DomainTestSuite.stopSupport();
    }

    @Test
    public void testServerStarted() throws IOException, MgmtOperationException {
        DomainLifecycleUtil domainMasterLifecycleUtil = domainTestSupport.getDomainMasterLifecycleUtil();
        DomainClient domainClient = domainMasterLifecycleUtil.getDomainClient();

        // create a NOTIFS file-handler
        ModelNode addFileHandlerOperation = new ModelNode();
        ModelNode fileHandlerAddress = new ModelNode();
        fileHandlerAddress.add("profile", "default");
        fileHandlerAddress.add("subsystem", "logging");
        fileHandlerAddress.add("file-handler", "notifications");
        addFileHandlerOperation.get(OP_ADDR).set(fileHandlerAddress);
        addFileHandlerOperation.get(OP).set(ADD);
        ModelNode file = new ModelNode();
        file.get(ModelDescriptionConstants.RELATIVE_TO).set("jboss.server.log.dir");
        file.get(ModelDescriptionConstants.PATH).set("notifications.log");
        addFileHandlerOperation.get(ModelDescriptionConstants.FILE).set(file);
        System.out.println("addFileHandlerOperation = " + addFileHandlerOperation);
        ModelNode result = domainClient.execute(addFileHandlerOperation);
        System.out.println("result = " + result);

        // add a logger for the category org.jboss.as.controller at the TRACE level
        // that use the NOTIFS handler
        ModelNode addHandler = new ModelNode();
        ModelNode loggerAddress = new ModelNode();
        loggerAddress.add("profile", "default");
        loggerAddress.add("subsystem", "logging");
        loggerAddress.add("logger", "org.jboss.as.controller");
        addHandler.get(OP_ADDR).set(loggerAddress);
        addHandler.get(OP).set("add-handler");
        addHandler.get("name").set("notifications");

        System.out.println("addHandler = " + addHandler);
        result = domainClient.execute(addHandler);
        System.out.println("result = " + result);

        domainClient.startServer("master", "main-one");
        waitUntilState(domainClient, mainOneConfigAddress, "STARTED");

        // check that the log for resource-added notification has been output
        ModelNode readLogFileOperation = new ModelNode();
        ///host=master/server=server-one/subsystem=logging:read-log-file(name=notifs.log, tail=true, lines=-1)
        ModelNode readLogFileAddress = new ModelNode();
        readLogFileAddress.add("host", "master");
        readLogFileAddress.add("server", "main-one");
        readLogFileAddress.add("subsystem", "logging");
        readLogFileOperation.get(OP_ADDR).set(readLogFileAddress);
        readLogFileOperation.get(OP).set("read-log-file");
        readLogFileOperation.get("name").set("notifications.log");
        readLogFileOperation.get("tail").set(true);
        readLogFileOperation.get("lines").set(-1);
        readLogFileOperation.get("skip").set(0);

        System.out.println("readLogFileOperation = " + readLogFileOperation);
        result = domainClient.execute(readLogFileOperation);
        System.out.println("result = " + result);

        ModelNode removeFileHandlerOperation = new ModelNode();
        removeFileHandlerOperation.get(OP_ADDR).set(fileHandlerAddress);
        removeFileHandlerOperation.get(OP).set(REMOVE);
        System.out.println("removeFileHandlerOperation = " + removeFileHandlerOperation);
        result = domainClient.execute(removeFileHandlerOperation);
        System.out.println("result = " + result);

        ModelNode removeHandler = new ModelNode();
        addHandler.get(OP_ADDR).set(loggerAddress);
        addHandler.get(OP).set("remove-handler");
        addHandler.get("name").set("notifications");
        result = domainClient.execute(removeHandler);
        System.out.println("result = " + result);

        fail("FAIL ON PURPOSE");
    }

    @Test
    public void testServerStopped() throws IOException {
        DomainLifecycleUtil domainMasterLifecycleUtil = domainTestSupport.getDomainMasterLifecycleUtil();
        DomainClient domainClient = domainMasterLifecycleUtil.getDomainClient();

        domainClient.startServer("master", "main-one");
        waitUntilState(domainClient, mainOneConfigAddress, "STARTED");

        domainClient.stopServer("master", "main-one", 10, SECONDS);
        waitUntilState(domainClient, mainOneConfigAddress, "DISABLED");

        fail("FAIL ON PURPOSE");
    }

    @Test
    public void testServerRestarted() {

    }

    @Test
    public void testServerKilled() {

    }

    @Test
    public void testServerDestroyed() {

    }
}

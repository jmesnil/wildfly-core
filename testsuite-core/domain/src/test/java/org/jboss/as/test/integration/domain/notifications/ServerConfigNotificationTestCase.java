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
import static org.jboss.as.test.integration.domain.management.util.DomainTestUtils.waitUntilState;
import static org.junit.Assert.fail;

import java.io.IOException;

import org.jboss.as.controller.client.helpers.domain.DomainClient;
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

    private static final ModelNode reloadOneConfigAddress = new ModelNode();

    static {
        // (host=slave),(server-config=reload-one)
        reloadOneConfigAddress.add("host", "master");
        reloadOneConfigAddress.add("server-config", "reload-one");
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

        domainClient.startServer("master", "reload-one");
        waitUntilState(domainClient, reloadOneConfigAddress, "STARTED");

        fail("FAIL ON PURPOSE");
    }

    @Test
    public void testServerStopped() throws IOException {
        DomainLifecycleUtil domainMasterLifecycleUtil = domainTestSupport.getDomainMasterLifecycleUtil();
        DomainClient domainClient = domainMasterLifecycleUtil.getDomainClient();

        domainClient.startServer("master", "reload-one");
        waitUntilState(domainClient, reloadOneConfigAddress, "STARTED");

        domainClient.stopServer("master", "reload-one", 10, SECONDS);
        waitUntilState(domainClient, reloadOneConfigAddress, "DISABLED");

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

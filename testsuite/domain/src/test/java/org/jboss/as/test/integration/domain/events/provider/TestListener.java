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

package org.jboss.as.test.integration.domain.events.provider;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;

import org.wildfly.extension.core.management.client.ControlledProcessStateListener;
import org.wildfly.extension.core.management.client.Process;

/**
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2016 Red Hat inc.
 */
public class TestListener implements ControlledProcessStateListener {

    private File file;
    private FileWriter fw;

    @Override
    public void init(Map<String, String> properties) {
        file = new File(properties.get("file"));
        try {
            fw = new FileWriter(file, true);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void cleanup() {
        try {
            fw.close();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            fw = null;
        }
    }

    @Override
    public void stateChanged(Process.Type processType, Process.RunningMode runningMode, Process.State oldState, Process.State newState) {
        try {
            System.err.println(String.format(">>>>>> %s %s %s %s\n", processType, runningMode, oldState, newState));
            fw.write(String.format("%s %s %s %s\n", processType, runningMode, oldState, newState));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

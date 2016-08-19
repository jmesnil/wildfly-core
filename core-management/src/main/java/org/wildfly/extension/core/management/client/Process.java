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

package org.wildfly.extension.core.management.client;

import org.jboss.as.controller.ControlledProcessState;
import org.jboss.as.controller.ProcessType;

/**
 * The overall information of a process (its state, running mode and type).
 *
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2016 Red Hat inc.
 */
/*
 * These enums duplicates the ones from the org.jboss.as.controller module to avoid leaking them
 * to the client API.
 */
public class Process {

    /**
     * The state of the process
     */
    public enum State {
        STARTING(ControlledProcessState.State.STARTING),
        RUNNING(ControlledProcessState.State.RUNNING),
        RELOAD_REQUIRED(ControlledProcessState.State.RELOAD_REQUIRED),
        RESTART_REQUIRED(ControlledProcessState.State.RESTART_REQUIRED),
        STOPPING(ControlledProcessState.State.STOPPING);

        private final ControlledProcessState.State state;

        State(ControlledProcessState.State state) {
            this.state = state;
        }


        @Override
        public String toString() {
            return state.toString();
        }
    }

    /**
     * The running mode of the process
     */
    public enum RunningMode {
        ADMIN_ONLY(org.jboss.as.controller.RunningMode.ADMIN_ONLY),
        NORMAL(org.jboss.as.controller.RunningMode.NORMAL);

        private final org.jboss.as.controller.RunningMode mode;

        RunningMode(org.jboss.as.controller.RunningMode mode) {
            this.mode = mode;
        }


        @Override
        public String toString() {
            return mode.toString();
        }
    }

    /**
     * The type of the process
     */
    public enum Type {
        DOMAIN_SERVER(ProcessType.DOMAIN_SERVER),
        EMBEDDED_SERVER(ProcessType.EMBEDDED_SERVER),
        STANDALONE_SERVER(ProcessType.STANDALONE_SERVER),
        HOST_CONTROLLER(ProcessType.HOST_CONTROLLER),
        EMBEDDED_HOST_CONTROLLER(ProcessType.EMBEDDED_HOST_CONTROLLER),
        APPLICATION_CLIENT(ProcessType.APPLICATION_CLIENT),
        SELF_CONTAINED(ProcessType.SELF_CONTAINED);

        private final ProcessType type;

        Type(ProcessType type) {
            this.type = type;
        }

        @Override
        public String toString() {
            return type.toString();
        }
    }
}

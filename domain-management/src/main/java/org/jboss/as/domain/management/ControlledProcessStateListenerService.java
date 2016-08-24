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

package org.jboss.as.domain.management;

import java.beans.PropertyChangeListener;
import java.util.Collection;
import java.util.Collections;

import org.jboss.as.controller.ControlledProcessState;
import org.jboss.as.controller.ControlledProcessStateService;
import org.jboss.as.controller.ProcessType;
import org.jboss.as.controller.RunningMode;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;

/**
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2016 Red Hat inc.
 */
public class ControlledProcessStateListenerService implements Service<Void> {
    private static final ServiceName SERVICE_NAME = ServiceName.JBOSS.append("core", "management", "process-state-listeners");

    private final InjectedValue<ControlledProcessStateService> controlledProcessStateService = new InjectedValue<>();
    private final PropertyChangeListener propertyChangeListener;
    private final Collection<ControlledProcessStateListener> listeners;

    public ControlledProcessStateListenerService(ProcessType processType, RunningMode runningMode, Collection<ControlledProcessStateListener> listeners) {
        this.listeners = Collections.unmodifiableCollection(listeners);
        propertyChangeListener = evt -> {
            System.out.println("ControlledProcessStateListenerService.ControlledProcessStateListenerService");
            System.out.println("evt = [" + evt + "]");
            if ("currentState".equals(evt.getPropertyName())) {
                ControlledProcessState.State oldState = (ControlledProcessState.State) evt.getOldValue();
                ControlledProcessState.State newState = (ControlledProcessState.State) evt.getNewValue();

                for(ControlledProcessStateListener listener: this.listeners) {
                    listener.stateChanged(processType, runningMode, oldState, newState);
                }
            }
        };
    }


    static void install(ServiceTarget serviceTarget, ProcessType processType, RunningMode runningMode, Collection<ControlledProcessStateListener> listeners) {
        System.err.println("ControlledProcessStateListenerService.install");
        System.err.println("serviceTarget = [" + serviceTarget + "], processType = [" + processType + "], runningMode = [" + runningMode + "], listeners = [" + listeners + "]");
        ControlledProcessStateListenerService service = new ControlledProcessStateListenerService(processType, runningMode, listeners);

        serviceTarget.addService(SERVICE_NAME, service)
                .addDependency(ControlledProcessStateService.SERVICE_NAME, ControlledProcessStateService.class, service.controlledProcessStateService)
                .install();
    }

    @Override
    public void start(StartContext context) throws StartException {
        System.err.println("ControlledProcessStateListenerService.start");
        controlledProcessStateService.getValue().addPropertyChangeListener(propertyChangeListener);
    }

    @Override
    public void stop(StopContext context) {
        System.err.println("ControlledProcessStateListenerService.stop");
        controlledProcessStateService.getValue().removePropertyChangeListener(propertyChangeListener);
        for (ControlledProcessStateListener listener: listeners) {
            listener.cleanup();
        }
    }

    @Override
    public Void getValue() throws IllegalStateException, IllegalArgumentException {
        return null;
    }
}

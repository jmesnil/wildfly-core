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

package org.wildfly.extension.core.management;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SERVICE;
import static org.wildfly.extension.core.management.CoreManagementExtension.PROCESS_STATE_LISTENERS_PATH;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.AbstractRemoveStepHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ObjectListAttributeDefinition;
import org.jboss.as.controller.ObjectTypeAttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PersistentResourceDefinition;
import org.jboss.as.controller.PropertiesAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleIdentifier;
import org.wildfly.extension.core.management.client.ControlledProcessStateListener;

/**
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2016 Red Hat inc.
 */
public class ProcessStateListenerResourceDefinition extends PersistentResourceDefinition {
    private static final String CLASS = "class";
    private static final String MODULE = "module";
    public static final PropertiesAttributeDefinition PROPERTIES = new PropertiesAttributeDefinition.Builder("properties", true)
            .setAllowExpression(true)
            .build();

    public static final ObjectTypeAttributeDefinition PROCESS_STATE_LISTENER = ObjectTypeAttributeDefinition.Builder.of("process-state-listener",
            SimpleAttributeDefinitionBuilder.create(CLASS, ModelType.STRING, false)
                    .setAllowExpression(false)
                    .build(),
            SimpleAttributeDefinitionBuilder.create(MODULE, ModelType.STRING, false)
                    .setAllowExpression(false)
                    .build(),
            PROPERTIES)
            .setRestartAllServices()
            .setAllowNull(true)
            .build();


    public static final AttributeDefinition PROCESS_STATE_LISTENERS = ObjectListAttributeDefinition.Builder.of("listeners", PROCESS_STATE_LISTENER)
            .setAllowNull(false)
            .setRuntimeServiceNotRequired()
            .build();

    private static AttributeDefinition[] ATTRIBUTES = new AttributeDefinition[] {
            PROCESS_STATE_LISTENERS
    };

    ProcessStateListenerResourceDefinition() {
        super(new Parameters(PROCESS_STATE_LISTENERS_PATH, CoreManagementExtension.getResourceDescriptionResolver(SERVICE, "process-state-listeners"))
                .setAddHandler(new ProcessStateListenerResourceDefinition.ProcessStateListenerAddHandler())
                .setRemoveHandler(new ProcessStateListenerResourceDefinition.ProcessStateListenerRemoveHandler()));
    }

    @Override
    public Collection<AttributeDefinition> getAttributes() {
        return Arrays.asList(ATTRIBUTES);
    }

    private static class ProcessStateListenerAddHandler extends AbstractAddStepHandler  {

        ProcessStateListenerAddHandler() {
            super(PROCESS_STATE_LISTENERS);
        }

        @Override
        protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
            List<ControlledProcessStateListener> listeners = new ArrayList<>();
            List<Map<String, String>> properties = new ArrayList<>();
            ModelNode listenersModel = PROCESS_STATE_LISTENERS.resolveModelAttribute(context, model);
            for (ModelNode listenerModel: listenersModel.asList()) {
                String className = listenerModel.get(CLASS).asString();
                String moduleIdentifier = listenerModel.get(MODULE).asString();
                ControlledProcessStateListener listener = newInstance(className, moduleIdentifier);

                listeners.add(listener);

                Map<String, String> props = PROPERTIES.unwrap(context, listenerModel);
                properties.add(props);
            }

            ControlledProcessStateListenerService.install(context.getServiceTarget(),
                    context.getProcessType(),
                    context.getRunningMode(),
                    listeners,
                    properties);
        }

        private static ControlledProcessStateListener newInstance(String className, String moduleIdentifier) throws OperationFailedException {
            ModuleIdentifier moduleID = ModuleIdentifier.fromString(moduleIdentifier);
            final Module module;
            try {
                module = Module.getContextModuleLoader().loadModule(moduleID);
                Class<?> clazz = module.getClassLoader().loadClass(className);
                System.out.println("clazz = " + clazz);
                System.out.println("expected = " + ControlledProcessStateListener.class);
                Object instance = clazz.newInstance();
                System.out.println("instance = " + instance);
                return ControlledProcessStateListener.class.cast(instance);
            } catch (Exception e) {
                throw new OperationFailedException(e.getMessage());
            }
        }
    }

    private static class ProcessStateListenerRemoveHandler  extends AbstractRemoveStepHandler {
        @Override
        protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
            context.removeService(ControlledProcessStateListenerService.SERVICE_NAME);
        }
    }
}

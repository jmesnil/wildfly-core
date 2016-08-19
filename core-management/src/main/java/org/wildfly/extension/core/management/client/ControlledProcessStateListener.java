package org.wildfly.extension.core.management.client;

import java.util.Map;

/**
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2016 Red Hat inc.
 */
@FunctionalInterface
public interface ControlledProcessStateListener {

    default void init(Map<String, String> properties) {
    }

    default void cleanup() {
    }

    void stateChanged(Process.Type processType, Process.RunningMode runningMode, Process.State oldState, Process.State newState);
}

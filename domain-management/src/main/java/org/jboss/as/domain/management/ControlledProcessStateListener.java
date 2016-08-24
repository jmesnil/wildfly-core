package org.jboss.as.domain.management;

import java.util.Map;

import org.jboss.as.controller.ControlledProcessState;
import org.jboss.as.controller.ProcessType;
import org.jboss.as.controller.RunningMode;

/**
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2016 Red Hat inc.
 */
@FunctionalInterface
public interface ControlledProcessStateListener {

    default void init(Map<String, String> properties) {
    }

    default void cleanup() {
    }

    void stateChanged(ProcessType processType, RunningMode runningMode, ControlledProcessState.State oldState, ControlledProcessState.State newState);
}

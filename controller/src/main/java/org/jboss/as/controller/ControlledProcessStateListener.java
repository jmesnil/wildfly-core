package org.jboss.as.controller;

import java.util.Properties;

/**
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2016 Red Hat inc.
 */
@FunctionalInterface
public interface ControlledProcessStateListener {

    default void init(Properties properties) {
    }

    void stateChanged(ControlledProcessState.State oldState, ControlledProcessState.State newState);
}

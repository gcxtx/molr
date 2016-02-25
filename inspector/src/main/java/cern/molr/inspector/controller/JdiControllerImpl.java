/*
 * © Copyright 2016 CERN. This software is distributed under the terms of the Apache License Version 2.0, copied
 * verbatim in the file “COPYING“.ing this licence, CERN does not waive the privileges and immunities granted
 * to it by virtue of its status as an Intergovernmental Organization or submit itself to any jurisdiction.
 */

package cern.molr.inspector.controller;

import cern.molr.commons.mole.GenericMoleRunner;
import cern.molr.commons.domain.Mission;
import cern.molr.inspector.entry.EntryListener;
import cern.molr.inspector.entry.EntryListenerFactory;
import com.sun.jdi.ThreadReference;
import com.sun.jdi.connect.IllegalConnectorArgumentsException;
import com.sun.jdi.connect.VMStartException;
import org.jdiscript.JDIScript;
import org.jdiscript.util.VMLauncher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.Optional;

/**
 * A controller for a JDI instance that can
 */
public class JdiControllerImpl implements JdiController, Closeable {

    private static final Logger LOGGER = LoggerFactory.getLogger(JdiControllerImpl.class);
    private static final String AGENT_RUNNER_CLASS = GenericMoleRunner.class.getName();

    private final JdiEntryRegistry<EntryListener> entryRegistry;
    private final JDIScript jdi;
    private final InhibitionWrapper flowInhibitionWrapper;
    private Runnable onClose;

    private JdiControllerImpl(JDIScript jdi, JdiEntryRegistry<EntryListener> registry, InhibitionWrapper flowInhibitionWrapper) {
        this.jdi = jdi;
        this.flowInhibitionWrapper = flowInhibitionWrapper;
        entryRegistry = registry;
        jdi.vmDeathRequest(event -> {
            entryRegistry.getEntryListener().ifPresent(EntryListener::onVmDeath);
            entryRegistry.unregister();
        });
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public void close() {
        onClose.run();
        jdi.vm().exit(0);
    }

    public InputStream getProcessError() {
        return jdi.vm().process().getErrorStream();
    }

    @Override
    public void setEntryListener(EntryListener entryListener) {
        // currently doing nothing, as handled through the entryRegistry
    }

    @Override
    public void stepForward() {
        ThreadReference threadReference = entryRegistry.getThreadReference()
                .orElseThrow(() -> new IllegalStateException("No active entry"));
        threadReference.resume();
    }

    @Override
    public void resume() {
        flowInhibitionWrapper.stopInhibiting();
        stepForward();
    }

    @Override
    public void terminate() {
        close();
    }

    public void setOnClose(Runnable onClose) {
        this.onClose = onClose;
    }

    private static class InhibitionWrapper {
        private boolean inhibiting = true;

        public boolean isInhibiting() {
            return inhibiting;
        }

        public void stopInhibiting() {
            inhibiting = false;
        }
    }

    public static class Builder {

        private static final String CLASSPATH_PREFIX = "-cp ";

        private String classPath;
        private Mission mission;
        private EntryListenerFactory<?> factory;

        public JdiControllerImpl build() throws IOException, IllegalConnectorArgumentsException, VMStartException {
            Objects.requireNonNull(classPath, "Classpath must be set");
            Objects.requireNonNull(factory, "Listener factory must be set");
            Objects.requireNonNull(mission, "Mission to inspect must be set");

            final String launchArguments = AGENT_RUNNER_CLASS + " " + mission.getMoleClassName() + " " + mission.getMissionContentClassName();
            final VMLauncher launcher = new VMLauncher(CLASSPATH_PREFIX + classPath, launchArguments);


            JdiEntryRegistry<EntryListener> entryRegistry = new JdiEntryRegistry<>();

            InhibitionWrapper flowInhibitionWrapper = new InhibitionWrapper();

            JDIScript jdi = new JdiInstanceBuilder()
                    .setLauncher(launcher)
                    .setMission(mission)
                    .setEntryRegistry(entryRegistry)
                    .setListenerFactory(factory)
                    .setFlowInhibitor(whatever -> flowInhibitionWrapper.isInhibiting())
                    .build();

            return new JdiControllerImpl(jdi, entryRegistry, flowInhibitionWrapper);
        }

        public Builder setListenerFactory(EntryListenerFactory<?> factory) {
            this.factory = factory;
            return this;
        }

        public Builder setClassPath(String path) {
            this.classPath = path;
            return this;
        }

        public Builder setMission(Mission method) {
            this.mission = method;
            return this;
        }

    }

}

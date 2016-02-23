/*
 * © Copyright 2016 CERN. This software is distributed under the terms of the Apache License Version 2.0, copied
 * verbatim in the file “COPYING”. In applying this licence, CERN does not waive the privileges and immunities granted
 * to it by virtue of its status as an Intergovernmental Organization or submit itself to any jurisdiction.
 */

package cern.molr.inspector.domain.impl;

import cern.molr.commons.domain.Mission;
import cern.molr.inspector.domain.InstantiationRequest;

/**
 * An immutable implementation of an {@link InstantiationRequest}.
 */
public class InstantiationRequestImpl implements InstantiationRequest {

    private final String classPath;
    private final Mission mission;

    /**
     * Creates a {@link InstantiationRequestImpl} using the given class path and {@link Mission}.
     *
     * @param classPath The class path containing zero or more paths separated by the {@link java.io.File#pathSeparator}.
     * @param mission   The mission to execute.
     */
    public InstantiationRequestImpl(String classPath, Mission mission) {
        this.classPath = classPath;
        this.mission = mission;
    }

    @Override
    public String getClassPath() {
        return classPath;
    }

    public Mission getMission() {
        return mission;
    }

}
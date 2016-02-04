/*
 * © Copyright 2016 CERN. This software is distributed under the terms of the Apache License Version 2.0, copied
 * verbatim in the file “COPYING”. In applying this licence, CERN does not waive the privileges and immunities granted
 * to it by virtue of its status as an Intergovernmental Organization or submit itself to any jurisdiction.
 */

package cern.jarrace.inspector.controller;

import cern.jarrace.inspector.entry.EntryListener;
import com.sun.jdi.ThreadReference;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * A registry for entries in a JDI instance.
 * @param <Listener> The type of {@link EntryListener}s the registry stores.
 */
public class JdiEntryRegistry<Listener extends EntryListener> {

    Map<String, ThreadReference> stateMap = new HashMap<>();
    Map<String, Listener> listenerMap = new HashMap<>();

    public Optional<ThreadReference> getThreadReference() {
        return stateMap.values().stream().findAny();
    }

    public Optional<ThreadReference> getThreadReference(String entry) {
        return Optional.ofNullable(stateMap.get(entry));
    }

    public Optional<Listener> getEntryListener() {
        return listenerMap.values().stream().findAny();
    }

    public Optional<Listener> getEntryListener(String entry) {
        return Optional.ofNullable(listenerMap.get(entry));
    }

    public void register(String entry, ThreadReference reference, Listener listener) {
        if (stateMap.containsKey(entry)) {
            throw new IllegalStateException("Entry already registered");
        }

        stateMap.put(entry, reference);
        listenerMap.put(entry, listener);
    }

}
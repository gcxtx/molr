/*
 * © Copyright 2016 CERN. This software is distributed under the terms of the Apache License Version 2.0, copied
 * verbatim in the file “COPYING”. In applying this licence, CERN does not waive the privileges and immunities granted
 * to it by virtue of its status as an Intergovernmental Organization or submit itself to any jurisdiction.
 */

package cern.molr.inspector.remote;

import cern.molr.commons.domain.Service;
import cern.molr.inspector.controller.JdiController;
import cern.molr.inspector.controller.JdiControllerImpl;
import cern.molr.inspector.entry.EntryListenerFactory;
import cern.molr.inspector.domain.InstantiationRequest;
import cern.molr.inspector.domain.InstantiationRequestImpl;
import cern.molr.inspector.json.ServiceTypeAdapter;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.sun.jdi.connect.IllegalConnectorArgumentsException;

import java.io.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * An entry point for creating a {@link JdiController} which communicates via
 * {@link System#in} and {@link System#out}. {@link System#err} is used to communicate errors from the process.
 */
public class SystemMain implements Closeable {

    private static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(Service.class, new ServiceTypeAdapter().nullSafe())
            .create();

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private JdiController controller;
    private JdiControllerReader commandReader;
    private EntryListenerWriter entryWriter;

    public SystemMain(JdiControllerImpl controller, JdiControllerReader commandReader, EntryListenerWriter entryWriter) {
        this.controller = controller;
        this.commandReader = commandReader;
        this.entryWriter = entryWriter;
        executor.submit(() -> {
            InputStream processError = controller.getProcessError();
            try (BufferedReader errorReader = new BufferedReader(new InputStreamReader(processError))) {
                while (true) {
                    logLine(errorReader);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    private static void logLine(BufferedReader reader) throws IOException {
        final String line = reader.readLine();
        if (line != null) {
            System.err.println(line);
        }
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            System.err.println("Expected 1 argument, but received " + args.length);
        } else {
            final InstantiationRequest request = GSON.fromJson(args[0], InstantiationRequestImpl.class);
            final PrintWriter outputWriter = new PrintWriter(System.out);
            final EntryListenerWriter writer = new EntryListenerWriter(outputWriter);

            final JdiControllerImpl controller = startJdi(request, (thread, state) -> writer);

            final BufferedReader inputReader = new BufferedReader(new InputStreamReader(System.in));
            final JdiControllerReader reader = new JdiControllerReader(inputReader, controller);
            final SystemMain main = new SystemMain(controller, reader, writer);
            controller.setOnClose(main::close);
        }
    }

    private static JdiControllerImpl startJdi(InstantiationRequest request, EntryListenerFactory<?> factory) throws Exception {
        try {
            return JdiControllerImpl.builder()
                    .setClassPath(request.getClassPath())
                    .setListenerFactory(factory)
                    .setService(request.getService())
                    .build();
        } catch (IllegalConnectorArgumentsException e) {
            System.err.println("Bad connection parameters " + request + " when starting JDIY:" + e);
            throw e;
        } catch (Exception e) {
            System.err.println("Failure when starting JDI instance:" + e);
            throw e;
        }
    }

    @Override
    public void close() {
        controller.terminate();
        commandReader.close();
        entryWriter.close();
        executor.shutdown();
    }
}
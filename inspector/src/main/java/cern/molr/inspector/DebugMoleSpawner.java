package cern.molr.inspector;

import cern.molr.commons.domain.Service;
import cern.molr.inspector.controller.JdiController;
import cern.molr.inspector.domain.InstantiationRequest;
import cern.molr.inspector.domain.InstantiationRequestImpl;
import cern.molr.inspector.domain.SourceFetcher;
import cern.molr.inspector.entry.EntryListener;
import cern.molr.inspector.json.ServiceTypeAdapter;
import cern.molr.inspector.remote.EntryListenerReader;
import cern.molr.inspector.remote.JdiControllerWriter;
import cern.molr.inspector.remote.SystemMain;
import cern.molr.jvm.JvmSpawnHelper;
import cern.molr.jvm.MoleSpawner;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * @author timartin
 */
public class DebugMoleSpawner implements MoleSpawner<JdiController>, SourceFetcher {

    private static final Logger LOGGER = LoggerFactory.getLogger(DebugMoleSpawner.class);
    private static final String CURRENT_CLASSPATH_VALUE = System.getProperty("java.class.path");
    private static final String INSPECTOR_MAIN_CLASS = SystemMain.class.getName();

    private static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(Service.class, new ServiceTypeAdapter().nullSafe())
            .create();

    private BufferedReader readerForClasspathFile(String filepath) {
        return new BufferedReader(new InputStreamReader(getClass()
                .getClassLoader()
                .getResourceAsStream(filepath)));
    }

    public String getSource(String classname) {
        try(BufferedReader reader = readerForClasspathFile(classname.replaceAll("\\.", "/") + ".java")) {
            return reader.lines()
                    .collect(Collectors.joining("\n"));
        } catch (IOException exception) {
            LOGGER.error("Could not load source code for class", classname);
            return "";
        }
    }

    @Override
    public JdiController spawnMoleRunner(Service service, String... args) throws Exception {
        InstantiationRequest request = new InstantiationRequestImpl(CURRENT_CLASSPATH_VALUE, service);
        String[] completedArgs = new String[args.length + 1];
        completedArgs[0] = GSON.toJson(request);
        int i = 1;
        for(String arg : args) {
            completedArgs[i++] = arg;
        }
        Process process = JvmSpawnHelper.getProcessBuilder(
                JvmSpawnHelper.appendToolsJarToClasspath(request.getClassPath()),
                INSPECTOR_MAIN_CLASS,
                completedArgs).start();
        redirectStream(process.getErrorStream(), System.err);
        Runtime.getRuntime().addShutdownHook(new Thread(process::destroy));
        return new MyJdiControllerWriter(process);
    }

    private static class MyJdiControllerWriter extends JdiControllerWriter {

        private final Process process;

        public MyJdiControllerWriter(Process process) {
            super(new PrintWriter(process.getOutputStream()));
            this.process = process;
        }

        @Override
        public void setEntryListener(EntryListener entryListener) {
            EntryListenerReader listenerReader = new EntryListenerReader(new BufferedReader(new InputStreamReader(process.getInputStream())), entryListener);
            listenerReader.setOnClose(process::destroy);
        }
    }

    @Override
    public JdiController spawnMoleRunner(Service service, String classpath, String... args) throws Exception {
        return null;
    }

    private static void redirectStream(InputStream input, PrintStream output) {
        Executors.newSingleThreadExecutor().submit(() -> {
            try (BufferedReader errorReader = new BufferedReader(new InputStreamReader(input))) {
                while (true) {
                    final String line = errorReader.readLine();
                    if (line != null) {
                        output.println(line);
                    }
                }
            } catch (IOException e) {
                output.println("Error when reading from process: " + e);
            }
        });
    }
}

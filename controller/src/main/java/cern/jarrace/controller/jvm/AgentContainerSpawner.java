package cern.jarrace.controller.jvm;

import java.io.IOException;

/**
 * Created by timartin on 27/1/2016.
 */
public interface AgentContainerSpawner {
    void spawnAgentContainer(String containerName, String jarPath) throws Exception;
}

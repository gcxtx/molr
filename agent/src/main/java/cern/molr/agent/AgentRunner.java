package cern.molr.agent;

/**
 * Created by jepeders on 1/22/16.
 */
public class AgentRunner {

    public static void main(String[] args) {
        if (args.length < 1) {
            throw new IllegalArgumentException("whatever");
        }

        final String agentName = args[0];
        final String entry = args[1];

        try {
            Agent agent = createAgent(agentName);
            agent.run(entry);
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("Done running");
    }

    private static Agent createAgent(String agentName) throws Exception {
        System.out.println("Running " + agentName);
        Class<Agent> clazz = (Class<Agent>) Class.forName(agentName);
        return (Agent) clazz.getConstructor().newInstance();
    }
}
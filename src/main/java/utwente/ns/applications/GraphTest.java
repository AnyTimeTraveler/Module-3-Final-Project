package utwente.ns.applications;

import utwente.ns.ui.NetworkGraph;

/**
 * Created by simon on 17.04.17.
 */
public class GraphTest implements IApplication {
    @Override
    public void start() {
        try {
            NetworkGraph.main();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

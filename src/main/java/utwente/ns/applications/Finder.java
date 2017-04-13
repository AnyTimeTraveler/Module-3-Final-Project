package utwente.ns.applications;

import utwente.ns.NetworkStack;

/**
 * Created by simon on 10.04.17.
 */
public class Finder implements IApplication {
    
    @Override
    public void start() {
        try {
            run();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private void run() throws Exception {
        NetworkStack network = new NetworkStack();
    }
}

package utwente.ns;

import org.reflections.Reflections;
import utwente.ns.applications.IApplication;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Scanner;

/**
 * @author rhbvkleef
 *         Created on 4/10/17
 */
public class Start {
    public static void main(String[] args) {
        Reflections applications = new Reflections("utwente.ns.applications");
        List<Class<? extends IApplication>> applicationClasses = new ArrayList<>(applications.getSubTypesOf(IApplication.class));

        System.out.println("Select the program thy wishes to allow to interfere with your life and take some of it away.\n");

        for (int i = 0; i < applicationClasses.size(); i++) {
            System.out.printf("%2d: %s\n", i, applicationClasses.get(i).getCanonicalName());
        }

        System.out.printf("\nThou shallth enter a number in tween of %d and %d, excluding %d: ",
                0, applicationClasses.size(), applicationClasses.size());

        int n = new Scanner(System.in).nextInt();
        if (n >= applicationClasses.size()) {
            System.out.println(Util.figlet(new String(Base64.getDecoder().decode("ZmlnbGV0IC13IDcwIFRob3Ugc2hhbGx0aCBiZSB0ZXJtaW5hdGVk"))));
            return;
        }

        Class<? extends IApplication> application = applicationClasses.get(n);

        try {
            application.newInstance().start();
        } catch (InstantiationException | IllegalAccessException e) {
            e.printStackTrace();
        }
    }
}

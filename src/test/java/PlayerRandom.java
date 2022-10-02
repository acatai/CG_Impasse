import java.util.ArrayList;
import java.util.Random;
import java.util.Scanner;

public class PlayerRandom
{
    public static void main(String[] args)
    {
        Random RNG = new Random(36);
        Scanner in = new Scanner(System.in);

        String color = in.next();
        System.err.println(color);

        while (true)
        {
            for (int y = 0; y < 8; ++y)
            {
                String line = in.next();
                System.err.println(line);
            }

            String last_action = in.next();
            System.err.println(last_action);

            int actions = in.nextInt();
            System.err.println(actions);
            ArrayList<String> legals = new ArrayList<>();
            for (int y = 0; y < actions; ++y)
            {
                String line = in.next();
                System.err.println(line);
                legals.add(line);
            }

            int i = RNG.nextInt(actions);
            System.out.println(legals.get(i) + " hello :)");
            //System.out.println("random");
        }
    }
}

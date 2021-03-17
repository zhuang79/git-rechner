import java.util.Scanner;

/**
 * Ein Taschenrechner
 * 
 * @author Malte Nagel, Prof. Dr.-Ing. Emre Cakar
 */
public class Rechner
{

    /*
     * Die main-Methode. Gibt "Hallo Welt!" aus.
     */
    public static void main(String[] args)
    {
        // Einrichtung
        int a, b, sum;
        Scanner in = new Scanner(System.in);
        
        // Eingabe
        System.out.println("Summe berechnen:");
        System.out.print("Erste Ganzzahl:");
        a = in.nextInt();
        System.out.print("Zweite Ganzzahl:");
        b = in.nextInt();
        
        // Verarbeitung
        sum = a + b;
        
        // Ausgabe
        System.out.printf("Summe: %d\n", sum);
    }

}

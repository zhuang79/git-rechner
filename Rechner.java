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
        JConsole console = new JConsole();
        
        while (true) {
            // Eingabe
            console.println("Summe berechnen:");
            a = console.readInt("Erste Ganzzahl: ");
            b = console.readInt("Zweite Ganzzahl: ");
            
            // Verarbeitung
            sum = a + b;
            
            // Ausgabe
            console.println("Summe: " + sum);
        }
    }

}

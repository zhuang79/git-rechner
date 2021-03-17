
// vi: ts=4 sw=4 et
/*
 * Klasse JConsole
 *
 * ==================================================================
 * History:
 * 2003-06-02 Swing-Implementierung von FrameIO
 * 2003-10-29 Cursor-Verhalten korrigiert
 * 2003-10-31 KeyListener korrekt implementiert. Alle read-Methoden
 *            werfen eine InterruptedException um ein Blockieren
 *            beim WindowClosing() zu verhindern!
 * 2003-11-14 Schriftaenderung Courier -> Monospaced (Euro-Zeichen ;-)
 * 2004-04-19 neue Methode: readFilename() incl. F3-Hotkey
 * 2004-04-21 neue Methoden: getFilename() / setPathname()
 * 2004-04-23 Dateiauswahl-Button, wenn Buttonleiste aktiviert
 * 2005-10-18 wirft keine InterruptedException mehr!
 * 2007-11-12 Konstruktor(b,h) korrigiert. Umlaute nun HTML-konform.
 * 2012-11-10 TextFilter nun mit javax.swing.jfilechooser.FilenameExtensionFilter
 *            String.format() statt NumberFormat
 *            readFilename(): pfad voranstellen, wenn nicht absoluter Pfad
 * 2012-11-16 pfad auf RZ-Pfade eingestellt (Linux/Windows)
 * 2013-09-15 Uebernahme der Methoden readArray()/readSequence() aus JArrayUtilities
 * ==================================================================
 */
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.filechooser.*;
import java.io.*;
import java.util.*;
import java.lang.reflect.*;

/**
 * Diese Klasse &ouml;ffnet ein Fenster, in dem Eingaben vom Benutzer
 * angefordert werden und formatierte Ausgaben erfolgen k&ouml;nnen.
 * <p>
 * In einer beiebigen Klasse ist hierzu ein Objekt dieser Klasse
 * anzulegen, also z.B.<p>
 * <pre>
 *     JConsole fenster = new JConsole();
 * </pre>
 * Anschlie&szlig;end kann mit den read-Methoden eine Eingabe vom Benutzer
 * gelesen werden, z.B.<p>
 * <pre>
 *     int i;
 *     
 *     i = fenster.readInt("Ganzzahl bitte: ");
 * </pre>
 * oder eine Ausgabe gemacht werden, z.B.<p>
 * <pre>
 *     fenster.print(i,8);
 *     fenster.print(umfang,7,3);
 * </pre>
 * 
 * @author Prof. Dr. Claus Hentschel
 * @version 2.0 (15.09.2013)
 */
public class JConsole extends JFrame
{
    private boolean closed = false;
    private JTextArea anzeige; // Anzeigebereich
    private JPanel buttonPanel;

    // Mehrere Konstruktoren, die fehlende Argumente durch
    // Standardwerte ersetzen
    /**
     * Standard-Konstruktor mit einer Breite von 500
     * und einer H&ouml;he von 400 Pixeln. Der Titel des Fensters lautet
     * <i>Java-Console</i>. In dem Fenster k&ouml;nnen Eingaben vom Benutzer
     * gemacht werden und es ist eine formatierte Ausgabe m&ouml;glich.
     */
    public JConsole() {
        this("Java-Console", false, 500, 400);
    }
    
    /**
     * Konstruktor, mit dem der Fenstertitel gesetzt wird.
     * Die Gr&ouml;&szlig;e des Fensters ist wie beim Standard-Konstruktor
     * 500x400 (BxH).
     * 
     * @param title Der Fenstertitel.
     */
    public JConsole(String title)
    {
        this(title, false, 500, 400);
    }
    
    /**
     * Konstruktor, mit dem der Fenstertitel gesetzt wird und der
     * im unteren Fensterbereich einen Knopf zum Beenden der Anwendung
     * einblendet.<p>
     * Die Gr&ouml;&szlig;e des Fensters ist wie beim Standard-Konstruktor
     * 500x400 (BxH).
     * 
     * @param title Der Fenstertitel.
     * @param mitButton <tt>true</tt>, um einen Ende-Knopf anzeigen zu lassen.
     */
    public JConsole(String title, boolean mitButton)
    {
        this(title, mitButton, 500, 400);
    }
    
    /**
     * Konstruktor der die Gr&ouml;&szlig;e des Fensters bestimmt. Der Titel des Fensters lautet
     * wie beim Standard-Konstruktor <i>Java-Console</i>.
     * 
     * @param b Die Breite des Fensters in Pixeln.
     * @param h Die H&ouml;he des Fensters in Pixeln.
     */
    public JConsole(int b, int h)
    {
        this("Java-Console", false, b, h);
    }
    
    /**
     * Konstruktor der die Gr&ouml;&szlig;e des Fensters und den Fenstertitelbestimmt.
     * 
     * @param title Der Fenstertitel.
     * @param b Die Breite des Fensters in Pixeln.
     * @param h Die H&ouml;he des Fensters in Pixeln.
     */
    public JConsole(String title, int b, int h)
    {
        this("Java-Console", false, b, h);
    }
    
    /**
     * Konstruktor der die Einstellung der Gr&ouml;&szlig;e, des Fenstertitels und der
     * Ende-Knopf-Anzeige erm&ouml;glicht.
     * 
     * @param title Der Fenstertitel.
     * @param mitButton <tt>true</tt>, um einen Ende-Knopf anzeigen zu lassen.
     * @param b Die Breite des Fensters in Pixeln.
     * @param h Die H&ouml;he des Fensters in Pixeln.
     */
    public JConsole(String title, boolean mitButton, int b, int h)
    {
        super(title);

        Container cp = getContentPane();
        cp.setLayout( new BorderLayout(10,10) );

        anzeige = new JTextArea();
        anzeige.setFont(new Font("Monospaced",Font.PLAIN,12));
        anzeige.setBackground( new Color(219,219,219) );
        anzeige.addKeyListener( new KeyObserver() );

        JScrollPane jsp = new JScrollPane( anzeige );
        cp.add("Center", jsp);

        // Ein Button zum 'aussergewoehnlichen' Beenden
        // falls gewuenscht!
        if (mitButton) {
            JButton bt = new JButton(" Beenden ");
            bt.addActionListener( new ButtonEnde() );
            buttonPanel = new JPanel();
            buttonPanel.setLayout( new GridLayout(1,1) );
            JPanel bpLinks = new JPanel();
            bpLinks.add(bt);
            buttonPanel.add(bpLinks);
            cp.add("South", buttonPanel );
        }
        
        // Den WindowListener anmelden, um unsere Anwendung
        // korrekt zu beenden.
        addWindowListener( new WindowEnde() );
        
        // Setzen des Standardpfads
        if (System.getProperty("os.name").startsWith("Windows")) {
            pfad = "G:/DOCS/mbau/hentschel/data";
        } else {
            pfad = "/vorlesungen/hentschel/data";
        }
        // Teste Existenz des Verzeichnisses
        File testpfad = new File(pfad);

        if (testpfad.exists() == false || testpfad.isDirectory() == false) {
            pfad = null;
        }

        // Setzen der Groesse des Fensters und Anzeige aktivieren
        setSize(b,h);
        setVisible(true);
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
    }

    // Ein WindowAdpater als innere Klasse
    // Ziel: Das Entfernen des Fenster-Objektes (Anwendungs-Ende)
    private class WindowEnde extends WindowAdapter {
        public void windowClosing( WindowEvent e) {
            exit();
        }
    }

    // Ein ActionListener als innere Klasse
    // Ziel: Anwendungs-Ende, wenn der Knopf gedrueckt wurde
    private class ButtonEnde implements ActionListener
    {
        // Die Methode, die die Schnittstelle ActionListener vorschreibt.
        // Notwendig fuer den Button
        public void actionPerformed( ActionEvent e ) {
            exit();
        }
    }

    /** Ver&auml;ndern der Hintergrundfarbe des Fensters. Standardwert ist
     * ein Grauton.
     * 
     * @param farbe Die neue Hintergrundfarbe
     */
    public void setBackground(Color farbe)  {
        super.setBackground(farbe);
        if (anzeige != null) {
            anzeige.setBackground(farbe);
        }
    }
    
    /**
     * Ver&auml;ndern der Schriftgr&ouml;&szlig;e (Standard: 12 Punkte).
     * 
     * @param punkte Gr&ouml;&szlig;e der Schrift (in Punkten).
     */
    public void setFontSize(int punkte) {
        anzeige.setFont(new Font("Monospaced",Font.PLAIN, punkte));
    }
    
    /**
     * Vergr&ouml;&szlig;ern der Eck-Eins&auml;tze (Insets), so dass am Rand
     * des Rahmens ein kleiner Abstand bleibt.
     * Das momentane Insets-Object (geholt von der Superklasse)
     * wird als Basis benutzt. Alle Werte werden als &Auml;nderung
     * aufaddiert!
     * 
     * Diese Methode ist nicht zum Aufrufen in der Anwendung gedacht.
     * Sie wird beim Erstellen der Oberfl&ouml;che von einem sogenannten
     * Layout-Manager genutzt.
     */
    // Laeuft neuerdings nicht unter Windows 8-( - deaktiviert
    public Insets getInsets() {
        Insets ecken = super.getInsets();

        if (System.getProperty("os.name").startsWith("Windows") == false) {
            ecken.top += 10;
            ecken.bottom += 10;
            ecken.left += 10;
            ecken.right += 10;
        }
        return ecken;
    }

    // ===============================================================
    // ==== OUTPUT SECTION (Instanz) =================================
    // ===============================================================

    /**
    * Gibt einen String in der TextArea aus.
    * <b>Dies ist die Basismethode aller folgenden Methoden!</b>
    * @param msg Der Text, der in der TextArea ausgegeben werden soll.
    */
    public void print(String msg) {
        anzeige.append(msg);
        anzeige.setCaretPosition(anzeige.getText().length());
    }

    /**
    * L&ouml;scht das Textfeld. <br>
    */
    public void clear() {
        anzeige.setText(""); // Anzeigefeld loeschen
        anzeige.setCaretPosition(anzeige.getText().length());
    }

    /**
    * Gibt einen String in der TextArea aus, dem ein Zeilenwechsel folgt.
    * @param msg Der Text, der in der TextArea ausgegeben werden soll.
    */
    public void println(String msg) { print(msg + "\n"); }

    /**
    * Gibt einen Zeilenwechsel in der TextArea aus.
    */
    public void println()           { print(""  + "\n"); }

    /**
    * Ausgabe einer Ganzzahl ohne Formatierung in der TextArea.
    * @param l Wert, der ausgegeben werden soll.
    */
    public void print(long l)        { print(l + ""); }

    /**
    * Ausgabe einer Ganzzahl ohne Formatierung in der TextArea,
    * der ein Zeilenwechsel folgt.
    * @param l Wert, der ausgegeben werden soll.
    */
    public void println(long l)      { print(l + "\n"); }

    /**
    * Ausgabe einer Gleitkommazahl ohne Formatierung in der TextArea.
    * @param d Wert, der ausgegeben werden soll.
    */
    public void print(double d)      { print(d + ""); }

    /**
    * Ausgabe einer Gleitkommazahl ohne Formatierung in der TextArea,
    * der ein Zeilenwechsel folgt.
    * @param d Wert, der ausgegeben werden soll.
    */
    public void println(double d)    { print(d + "\n"); }

    /**
    * Ausgabe eines Zeichens in der TextArea.
    * @param c Wert, der ausgegeben werden soll.
    */
    public void print(char c)        { print(c + ""); }

    /**
    * Ausgabe eines Zeichens in der TextArea, dem ein Zeilenwechsel folgt.
    * @param c Wert, der ausgegeben werden soll.
    */
    public void println(char c)      { print(c + "\n"); }

    /**
    * Ausgabe eines Wahrheitswertes in der TextArea.
    * @param b Wert, der ausgegeben werden soll.
    */
    public void print(boolean b)     { print(b + ""); }

    /**
    * Ausgabe eines Wahrheitswertes in der TextArea, dem ein Zeilenwechsel folgt.
    * @param b Wert, der ausgegeben werden soll.
    */
    public void println(boolean b)   { print(b + "\n"); }

    /**
    * Ausgabe eines beliebigen Objektes in der TextArea.
    * @param o Objekt, das ausgegeben werden soll.
    */
    public void print(Object o)      { print(o + ""); }

    /**
    * Ausgabe eines beliebigen Objektes in der TextArea, dem ein Zeilenwechsel
    * folgt.
    * @param o Objekt, das ausgegeben werden soll.
    */
    public void println(Object o)    { print(o + "\n"); }

    /**
    * Gibt einen String in angegebener L&auml;nge in der TextArea aus.
    * Ist der String k&uuml;rzer, als die angegebene L&auml;nge, so werden
    * f&uuml;hrende Leerzeichen ausgegeben.<b>
    * Der String wird also stets <b>rechtsb&uuml;ndig</b> ausgegeben!
    * <b>Dies ist die Basismethode aller folgenden Methoden!</b>
    * @param s Der auszugebende Text als String
    * @param len Die L&auml;nge der Ausgabe als Ganzzahl
    */
    public void print(String s, int len)  {
        int strlen = s.length();

        for (int i=0; i < len - strlen; i++)
            s = " " + s;

        print(s);
    }

    /**
    * Rechtsb&uuml;ndige Ausgabe eines Strings in angegebener L&auml;nge
    * in der TextArea, dem ein Zeilenwechsel folgt.
    * @param s Der auszugebende Text als String
    * @param len Die L&auml;nge der Ausgabe als Ganzzahl
    */
    public void println(String s, int len)  { print(s, len); print("\n"); }
    
    /**
    * Rechtsb&uuml;ndige Ausgabe einer Ganzahl in angegebener L&auml;ngen
    * in der TextArea.
    * @param l Der auszugebende Wert
    * @param len Die L&auml;nge der Ausgabe als Ganzzahl
    */
    public void print(long l,    int len)  { print(l + "", len); }

    /**
    * Rechtsb&uuml;ndige Ausgabe einer Ganzahl in angegebener L&auml;ngen
    * in der TextArea, dem ein Zeilenwechsel folgt.
    * @param l Der auszugebende Wert
    * @param len Die L&auml;nge der Ausgabe als Ganzzahl
    */
    public void println(long l,    int len)  { print(l,  len); print("\n"); }

    /**
    * Rechtsb&uuml;ndige Ausgabe einer Gleitkommazahl in angegebener L&auml;ngen
    * in der TextArea.
    * @param d Der auszugebende Wert
    * @param len Die L&auml;nge der Ausgabe als Ganzzahl
    */
    public void print(double d,  int len)  { print(d + "", len); }

    /**
    * Rechtsb&uuml;ndige Ausgabe einer Gleitkommazahl in angegebener L&auml;ngen
    * in der TextArea, dem ein Zeilenwechsel folgt.
    * @param d Der auszugebende Wert
    * @param len Die L&auml;nge der Ausgabe als Ganzzahl
    */
    public void println(double d,  int len)  { print(d,  len); print("\n"); }

    /**
    * Rechtsb&uuml;ndige Ausgabe eines Wahrheitswertes in angegebener
    * L&auml;ngen in der TextArea.
    * @param b Der auszugebende Wert
    * @param len Die L&auml;nge der Ausgabe als Ganzzahl
    */
    public void print(boolean b, int len)  { print(b + "", len); }

    /**
    * Rechtsb&uuml;ndige Ausgabe eines Wahrheitswertes in angegebener
    * L&auml;ngen in der TextArea, dem ein Zeilenwechsel folgt.
    * @param b Der auszugebende Wert
    * @param len Die L&auml;nge der Ausgabe als Ganzzahl
    */
    public void println(boolean b, int len)  { print(b,  len); print("\n"); }

    /**
    * Rechtsb&uuml;ndige Ausgabe eines Zeichens in der angegebenen Anzahl
    * in der TextArea. (z,B. f&uuml;r eine Linie)
    * @param c Das auszugebende Zeichen
    * @param len Die L&auml;nge der Ausgabe als Ganzzahl
    */
    public void print(int count, char c)  {
        if ( count <= 0 ) return; // Muss positiv sein!
        for (int i = 0; i < count; i++ )
            print(c + "");
    }
    /**
    * Rechtsb&uuml;ndige Ausgabe eines Zeichens in angegebener L&auml;ngen
    * in der TextArea.
    * @param c Das auszugebende Zeichen
    * @param len Die L&auml;nge der Ausgabe als Ganzzahl
    */
    public void print(char c,    int len)  { print(c + "", len); }

    /**
    * Rechtsb&uuml;ndige Ausgabe eines Zeichens in angegebener L&auml;ngen
    * in der TextArea, dem ein Zeilenwechsel folgt.
    * @param c Das auszugebende Zeichen
    * @param len Die L&auml;nge der Ausgabe als Ganzzahl
    */
    public void println(char c,    int len)  { print(c,  len); print("\n"); }

    /**
    * Rechtsb&uuml;ndige Ausgabe eines Objektes in angegebener L&auml;ngen
    * in der TextArea.
    * @param o Das auszugebende Objekt
    * @param len Die L&auml;nge der Ausgabe als Ganzzahl
    */
    public void print(Object o,  int len)  { print(o + "", len); }

    /**
    * Rechtsb&uuml;ndige Ausgabe eines Objektes in angegebener L&auml;ngen
    * in der TextArea, dem ein Zeilenwechsel folgt.
    * @param o Das auszugebende Objekt
    * @param len Die L&auml;nge der Ausgabe als Ganzzahl
    */
    public void println(Object o,  int len)  { print(o,  len); print("\n"); }

    /**
    * Ausgabe von Gleitkommazahlen mit einer bestimmten L&auml;nge und
    * einer bestimmten Anzahl von Nachkommastellen in der TextArea.
    * @param d Wert, der ausgegeben werden soll
    * @param len L&auml;nge der Ausgabe
    * @param prec Anzahl der Nachkommastellen
    */
    public void print(double d, int len, int prec) {
        String         out;            // Ausgabe als String

        if (Double.isNaN(d) || Double.isInfinite(d))
            out = "" + d;
        else {
            String format = String.format("%%%d.%df", len, prec);
            out = String.format(format, d);;
        }
        print(out, len);
    }

    /**
    * Ausgabe von Gleitkommazahlen mit einer bestimmten L&auml;nge und
    * einer bestimmten Anzahl von Nachkommastellen in der TextArea,
    * dem ein Zeilenwechsel folgt.
    * @param d Wert, der ausgegeben werden soll
    * @param len L&auml;nge der Ausgabe
    * @param prec Anzahl der Nachkommastellen
    */
    public void println(double d, int len, int prec) {
        print(d, len, prec);
        print("\n");
    }

// ===============================================================
// ==== INPUT SECTION ============================================
// ===============================================================

    /**
     * Ausgabe eines Textes und Einlesen einer Zeile als String!
     * @param prompt auszugebender Text
     * @return die eingegebene Zeile als String
     */
    public String readLine(String prompt) {
        String answer = null;
        
        try {
            answer = getLine(prompt);
        }
        catch (InterruptedException ie) {
            System.exit(0);
        }
        
        if (answer == null)
            return "";
        return answer;
    }

    /**
     * Einlesen einer Zeile als String!
     * @return die eingegebene Zeile als String
     */
    public String readLine() { return readLine(""); }

    /**
     * Ausgabe eines Textes und Einlesen einer Zeichenkette (String)!
     * @param prompt auszugebender Text
     * @return der eingegebene Text als String
     */
    public String readString(String prompt) {
        return readLine(prompt);
    }

    /**
     * Einlesen einer Zeichenkette (String)!
     * @return der eingegebene Text als String
     */
    public String readString() { return readString(""); }

    /**
     * Ausgabe eines Textes und Einlesen eines Zeichens (char)!
     * @param prompt auszugebender Text
     * @return das eingegebene Zeichen als char
     */
    public char readChar(String prompt) {
        String input = readLine(prompt);

        if (input != null && input.length() > 0)
            return input.charAt(0);
        return '\n';
    }

    /**
     * Einlesen eines Zeichens (char)!
     * @return das eingegebene Zeichen als char
     */
    public char readChar() { return readChar(""); }

    /**
     * Ausgabe eines Textes und Einlesen eines Wahrheitswertes (boolean)! <br>
     * Als Wahrheitswert k&ouml;nnen diverse Zeichenketten eingegeben werden.
     * Siehe hierzu die Auflistung bei den R&uuml;ckgabewerten!
     * @param prompt auszugebender Text
     * @return
     *     true:  Bei Eingabe von true, yes, ja , t, j oder 1 <br>
     *     false: Bei Eingabe von false, no, nein, f, n oder 0 
     */
    public boolean readBoolean(String prompt) {
        boolean result = false;
        boolean noValue;
        String input;

        do {
            noValue = false;
            input = readLine(prompt);

            if (input == null || input.equals(""))
                return result;

            input = input.toUpperCase();
            
            if (input.equals("TRUE") || input.equals("YES") || input.equals("JA") ||
                    input.equals("T") || input.equals("Y") || input.equals("J") ||
                    input.equals("1")) {
                result = true;
            } else if (input.equals("FALSE") || input.equals("NO") || input.equals("NEIN") ||
                    input.equals("F")       || input.equals("N")     ||
                    input.equals("0")) {
                result = false;
            } else {
                noValue = true;
                prompt    = "Fehler! Bitte einen Wahrheitswert: ";
            }
        }
        while (noValue);

        return result;
    }

    /**
     * Einlesen eines Wahrheitswertes (boolean)! <br>
     * Als Wahrheitswert k&ouml;nnen diverse Zeichenketten eingegeben werden.
     * Siehe hierzu die Auflistung bei den R&uuml;ckgabewerten!
     * @return
     *     true:  Bei Eingabe aus : true True TRUE yes Yes YES ja Ja JA t T y Y j J 1 <br>
     *     false: Bei Eingabe aus : false False FALSE no No NO nein Nein NEIN f F n N 0 
     */
    public boolean readBoolean() { return readBoolean(""); }

    /**
     * Ausgabe eines Textes und Einlesen einer Ganzzahl (long)!
     * @param prompt auszugebender Text
     * @return das eingegebene Wert als long
     */
    public long readLong(String prompt) {
        long result = 0L;
        boolean noValue;

        do {
            noValue = false;

            try {
                String answer = readLine(prompt);
                if (answer == null || answer.equals(""))
                    return result;
                result = (new Long(answer)).longValue();
            } catch (NumberFormatException ex) {
                noValue = true;
                prompt = "Fehler! Bitte eine Ganzzahl: ";
            }
        }
        while (noValue);

        return result;
    }

    /**
     * Einlesen einer Ganzzahl (long)!
     * @return das eingegebene Wert als long
     */
    public long readLong() { return readLong(""); }

    /**
     * Ausgabe eines Textes und Einlesen einer Ganzzahl (int)!
     * @param prompt auszugebender Text
     * @return das eingegebene Wert als int
     */
    public int readInt(String prompt) { return (int) readLong(prompt); }

    /**
     * Einlesen einer Ganzzahl (int)!
     * @return das eingegebene Wert als int
     */
    public int readInt() { return readInt(""); }
    
    /**
     * Ausgabe eines Textes und Einlesen einer Ganzzahl (short)!
     * @param prompt auszugebender Text
     * @return das eingegebene Wert als short
     */
    public short readShort(String prompt) { return (short) readLong(prompt); }

    /**
     * Einlesen einer Ganzzahl (short)!
     * @return das eingegebene Wert als short
     */
    public short readShort() { return readShort(""); }
    
    /**
     * Ausgabe eines Textes und Einlesen einer Gleitkommazahl (double)!
     * @param prompt auszugebender Text
     * @return das eingegebene Wert als double
     */
    public double readDouble(String prompt) {
        double result = 0.0;
        boolean noValue;

        do {
            noValue = false;

            try {
                String answer = readLine(prompt);
                if (answer == null || answer.equals(""))
                    return result;
                result = (new Double(answer)).doubleValue();
            } catch (NumberFormatException ex) {
                noValue = true;
                prompt   = "Fehler! Bitte eine Gleitkommazahl: ";
            }
        }
        while (noValue);

        return result;
    }

    /**
     * Einlesen einer Gleitkommazahl (double)!
     * @return das eingegebene Wert als double
     */
    public double readDouble() { return readDouble(""); }

    /**
     * Ausgabe eines Textes und Einlesen einer Gleitkommazahl (float)!
     * @param prompt auszugebender Text
     * @return das eingegebene Wert als float
     */
    public float readFloat(String prompt) { return (float) readDouble(prompt); }

    /**
     * Einlesen einer Gleitkommazahl (float)!
     * @return das eingegebene Wert als float
     */
    public float readFloat() { return readFloat(""); }
    
// ===============================================================
// ==== GET FILENAME SECTION =====================================
// ===============================================================

    private JButton fileButton;
    private JPanel bpRechts;
    
    /**
     * Ausgabe eines Textes und Einlesen eines Dateinamens (String)!
     * @param prompt auszugebender Text
     * @return der Dateiname als String
     */
    public String readFilename(String prompt) {
        if (buttonPanel != null) {
            bpRechts = new JPanel();
            fileButton = new JButton("Dateiauswahl");
            fileButton.addActionListener( new Dateiauswahl() );
            GridLayout gl = (GridLayout) buttonPanel.getLayout();
            gl.setColumns(2);
            bpRechts.add(fileButton);
            buttonPanel.add(bpRechts);
            buttonPanel.revalidate();
        }
        hotkey = true;
        String name = readLine(prompt);

        if (buttonPanel != null) {
            buttonPanel.remove(bpRechts);
            bpRechts = null;
            fileButton = null;
            GridLayout gl = (GridLayout) buttonPanel.getLayout();
            gl.setColumns(1);
            buttonPanel.revalidate();
        }
        
        if (name.length() == 0) {
            return null;
        }

        // kein Pfad gesetzt!
        if (pfad == null || pfad.isEmpty()) {
            return name;
        }

        // Dateiname mit Verzeichnisangabe
        if (name.contains("/")) {
            return name;
        }
        
        // Dateiname mit Laufwerk
        if (System.getProperty("os.name").startsWith("Windows") && name.contains(":")) {
            return name;
        }
        
        return pfad + "/" + name;
    }

    /**
     * Einlesen eines Dateinamens (String)!
     * @return der Dateiname als String
     */
    public String readFilename() { return readFilename(""); }

    /**
     * Ermitteln eines Dateinamens in einem Dialogfenster!
     * @return der Dateiname als String
     */
    public String getFilename() {    
        JFileChooser dialog = new JFileChooser( pfad ); // Ein Object zur Dateiauswahl

        dialog.setFileFilter(new FileNameExtensionFilter("Textdateien", "txt"));

        // Fenster anzeigen! (Auswahl bestaetigt?)
        if ( dialog.showOpenDialog( this ) != JFileChooser.APPROVE_OPTION )
            return null; // Keine Datei ausgewaehlt worden!
            
        // Dateiname zurueckgeben
        return dialog.getSelectedFile().getAbsolutePath();
    }
    
    /**
     * Einlesen eines Dateinamens (String)!
     * @param path Der Pfadname, der im Dialogfenster zu Anfang genutzt wird.
     */
    public void setPathname( String pathname ) {
        pfad = pathname;
    }

    // Ein ActionListener als innere Klasse
    // Ziel: DateiauswahlDialog, wenn der Knopf gedrueckt wurde
    private class Dateiauswahl implements ActionListener
    {
        // Die Methode, die die Schnittstelle ActionListener vorschreibt.
        // Notwendig fuer den Button
        public void actionPerformed( ActionEvent e ) {
            fetchFilename();
        }
    }

    synchronized private void fetchFilename() {
        // Dateiname ermitteln und in Anzeige einfuegen!
        String dateiname = getFilename();
        
        // Auswahl abgebrochen?
        if (dateiname != null) {
            // Anzeigefenster anpassen, so als ob der Benutzer die Eingabe
            // ueber Tastatur gemacht hat.
            int laenge = ibuffer.length();
            int ende = anzeige.getText().length();
            anzeige.replaceRange( dateiname, ende - laenge, ende);
            anzeige.append("\n");
        
            // Dateiname incl Pfad in Instanzvariable uebernehmen!!
            ibuffer = new StringBuffer( dateiname );
        
            // Newline-Taste melden (Ende des Einlesens eines Dateinamens)
            chr = '\n';
            char_ready = true;
        } else {
            chr = 1; // Pseudocode, zur Erkennung in getLine()
        }
        notify();
    }
    
// ===============================================================
// ==== INTERNAL INPUT SECTION ===================================
// ===============================================================

    private StringBuffer ibuffer; // Sammeln der Character
    private char chr;
    private boolean char_ready = false;
    private boolean hotkey = false; // Fuer eine Dateiauswahl in einem Fenster
    private String pfad;

    // Lesen einer Zeile (als String)
    private String getLine(String prompt) throws InterruptedException {
        String answer = "";
        char c; // Der Tastencode

        print(prompt); // Prompt ausgeben in Textarea
            
        // StringBuffer erzeugen
        ibuffer = new StringBuffer();

        do {
            // Holen des Tastencodes
            if ((c = read()) == 0) {
                throw new InterruptedException("Ende angefordert");
            }

            if (c == 8) { // Backspace-Taste?
                if ( ibuffer.length() > 0 ) {
                    ibuffer.deleteCharAt(ibuffer.length() - 1);
                }
                continue;
            }
            
            if (c != '\n' && c != 1) {
                ibuffer.append(c);
            }
        }
        while ( c != '\n' );

        // Ergebnis umwandeln in einen String
        answer = ibuffer.toString();
        hotkey = false;
        return answer;
    }

    /**
     * Entfernen des Fensters.
     */
    synchronized public void exit() {
        chr = 0;
        char_ready = true;
        notify();

        setVisible(false);
        dispose();
    }
    
    // Synchronisierter Empfang eines Zeichens der Tastatur
    synchronized private char read () {
        if (!char_ready) {
            try {
                wait ();
            }
            catch (Exception e) { }
        }
        char_ready = false;
        return chr;
    }

    // Innere Klasse, die fuer die spezielle Tastaturbehandlung verantwortlich ist
    // Die Methoden muessen mit der read()-Methode synxhronisiert werden, die ja auf
    // einen Tastendruck wartet!
    private class KeyObserver extends KeyAdapter
    {
        // Erkennen des HOTKEY F3, um einen Dateiauswahl-Dialog zu starten
        public void keyReleased( KeyEvent e ) {
            if (hotkey == false || char_ready)
                return;
        
            // Tastendruck F3 erkannt?
            if ( e.getKeyCode() == KeyEvent.VK_F3 ) {
                fetchFilename();
                e.consume();
            }
        }
    
        // Eine (normale Taste wurde gedrueckt. Diese Taste muss von der read()-Methode
        // weiterverarbeitet werden. Solange dies nicht beendet ist (char_ready) wird
        // keine weitere Taste angenommen!
        public void keyTyped (KeyEvent e) {
            if (char_ready)
                return;
            fetchKey( e. getKeyChar() );
        }
    }
    
    synchronized private void fetchKey( char c ) {
        chr = c;
        char_ready = true;
        notify();
    }

// ===============================================================
// ==== FILE INPUT SECTION (object) ==============================
// ===============================================================

    /*
     * Uebernommen zum WS 2013/14 aus der Klasse JArrayUtilities.
     * 
     * Hilfsfunktionen zum Einlesen eines Arrays aus einer Datei sowie eine Methode
     * zum Veraendern eines Arrays (removeLastColumn())
     */

    final static String stdDelim = " \t,;!?:()[]{}<>*/";

    /**
     * Liest Zahlenwerte aus einer Textdatei in ein zweidimensionales Array
     * vom Datentyp double. Jede Zeile in der Datei wird als Array-Zeile aufgefasst.
     * wenn sie die gleiche Anzahl Zahlen wie die erste Zeile (mit mehr als einer Zahl)
     * enthaelt.
     * <p>
     * Um Zahlen zu erkennen, werden alle Whitespace-Zeichen (Leerzeichen, Tabulator) sowie
     * alle Satzzeichen (ausser Punkt!) und alle Klammern (rund, eckig, spitz, geschweift) als
     * Trennzeichen angesehen.
     * 
     * @param filename Zeichenkette mit dem Namen der Datei.
     * @return Das zweidimensionale Array vom Datentype double. Oder null im Fehlerfall.
     */
    public double[][] readArray(String filename) throws IOException
    {
        return readArray(filename, "", true);
        
    }
    
    /**
     * Liest Zahlenwerte aus einer Textdatei in ein zweidimensionales Array
     * vom Datentyp double. Jede Zeile in der Datei wird als Array-Zeile aufgefasst.
     * wenn sie die gleiche Anzahl Zahlen wie die erste Zeile (mit mehr als einer Zahl)
     * enthaelt.
     * 
     * @param filename Zeichenkette mit dem Namen der Datei.
     * @param delim  Zeichenkette mit allen Trennzeichen (zu ignorierenden Zeichen) in der Datei.
     * @param appendDelim Die uebergebenen Trennzeichen sollen zusaetzlich zu den Standardtrennzeichen benutzt werden.
     * @return Das zweidimensionale Array vom Datentype double. Oder null im Fehlerfall.
     */
    public double[][] readArray(String filename, String delim, boolean appendDelim) throws IOException
    {
        double a[][] = null; // Das Array (vorerst noch "leer")
        int n = 0; // Zeilenanzahl des Array
        int m = 0; // Spaltenanzahl des Arrays
        int z = 0; // Zeilenanzahl der Datei (Zeilen mit m Zahlen)
        BufferedReader br = new BufferedReader(new FileReader(filename));
        StringTokenizer st; // Zum Zerlegen der Zeile in Token
        String line; // Eine Zeile der Datei
        int ta; // Tokenanzahl
        String useDelim = new String("");
        
        if (appendDelim == true) {
            useDelim = stdDelim;
        }
        useDelim += delim;
        
        while ((line = br.readLine()) != null) {
            // Leere Zeilen und Kommantarzeilen ueberlesen
            if (line.length() < 1 || line.charAt(0) == '#') {
                continue;
            }
            
            // Zeile in Token zerlegen
            st = new StringTokenizer(line,  useDelim);
            ta = 0; // Tokenanzahl vorerst 0
            
            // Suche nach Token, die Zahlen sind
            while (st.hasMoreTokens()) {
                try {
                    Double.parseDouble(st.nextToken());
                    ta++;
                }
                catch (NumberFormatException nfe) {}
            }
            
            // Tokenanzahl muss mindestens 2 sein
            if (ta < 2) {
                continue;
            }
            
            // Wenn Spaltenanzahl noch 0 ist, dann ist ta die Spaltenzahl m
            // Die Spaltenanzahl muss mit der Tokenanazhal uebereinstimmen, d.h.
            // es werden nur Zeilen gelesen, die die gleiche Anzahl von Zahlen
            // haben!
            if (m == 0) {
                m = ta;
            }
            else if (ta != m) {
                continue;
            }
            
            // Array anlegen, falls es noch nicht existiert
            if (a == null) {
                a = new double[1][m]; // Definition der Matrix
                n = 1; // Zeilenanzahl ist nun 1
            }
            
            // Arraygroesse verdoppeln, falls mehr Zeilen enthalten
            if (z >= n) {
                // Zeilenanzahl erhoehen
                a = (double[][]) resizeArray(a, n+1);
                n++; // Zeilenanzahl erhoehen
            }
            
            st = new StringTokenizer(line, useDelim);

            // Versuche m Spaltenwerte der Zeile z einzulesen
            for (int s = 0; s < m; ) {
                try {
                    a[z][s] = Double.parseDouble(st.nextToken());
                }
                catch (NumberFormatException e) {
                    continue;
                }
                s++;
            }
            // Zeilenzaehler inkrementieren!
            z++;
        }
        // Wenn alles erfolgreich war, muss z mindestens 1 sein!
        if (z < 1) {
            return null;
        }
        // Datei schliessen
        br.close();
        return a;
    }
    
    /**
     * Liest Zahlenwerte aus einer Textdatei in ein eindimensionales Array
     * vom Datentyp double. Jede Zeile in der Datei wird als Array-Zeile aufgefasst.
     * wenn sie genau eine Zahl enthaelt.
     * <p>
     * Um Zahlen zu erkennen, werden alle Whitespace-Zeichen (Leerzeichen, Tabulator) sowie
     * alle Satzzeichen (ausser Punkt!) und alle Klammern (rund, eckig, spitz, geschweift) als
     * Trennzeichen angesehen.
     * 
     * @param filename Zeichenkette mit dem Namen der Datei.
     * @return Das zweidimensionale Array vom Datentype double. Oder null im Fehlerfall.
     */
    public double[] readSequence(String filename) throws IOException
    {
        return readSequence(filename, "", false);
        
    }
    
    /**
     * Liest Zahlenwerte aus einer Textdatei in ein eindimensionales Array
     * vom Datentyp double. Jede Zeile in der Datei wird als Array-Zeile aufgefasst.
     * wenn sie genau eine Zahl enthaelt.
     * 
     * @param filename Zeichenkette mit dem Namen der Datei.
     * @param delim  Zeichenkette mit allen Trennzeichen (zu ignorierenden Zeichen) in der Datei.
     * @param appendDelim Die uebergebenen Trennzeichen sollen zusaetzlich zu den Standardtrennzeichen benutzt werden.
     * @return Das zweidimensionale Array vom Datentype double. Oder null im Fehlerfall.
     */
    public double[] readSequence(String filename, String delim, boolean appendDelim) throws IOException
    {
        double a[] = null; // Das Array (vorerst noch "leer")
        int n = 0; // Zeilenanzahl des Array
        int z = 0; // Zeilenanzahl der Datei (Zeilen mit 1 Zahl)
        BufferedReader br = new BufferedReader(new FileReader(filename));
        StringTokenizer st; // Zum Zerlegen der Zeile in Token
        String line; // Eine Zeile der Datei
        int ta; // Tokenanzahl
        String useDelim = new String("");
        
        if (appendDelim == true) {
            useDelim = stdDelim;
        }
        useDelim += delim;
        
        while ((line = br.readLine()) != null) {
            // Leere Zeilen und Kommantarzeilen ueberlesen
            if (line.length() < 1 || line.charAt(0) == '#') {
                continue;
            }
            
            // Zeile in Token zerlegen
            st = new StringTokenizer(line, useDelim);
            ta = 0; // Tokenanzahl vorerst 0
            
            // Suche nach Token, die Zahlen sind
            while (st.hasMoreTokens() && ta < 2) {
                try {
                    Double.parseDouble(st.nextToken());
                    ta++;
                }
                catch (NumberFormatException nfe) {}
            }
            
            // Tokenanzahl muss geanu 1 sein
            if (ta != 1) {
                continue;
            }
            
            // Array anlegen, falls es noch nicht existiert
            if (a == null) {
                a = new double[1]; // Definition der Matrix
                n = 1; // Zeilenanzahl ist nun 1
            }
            
            // Arraygroesse verdoppeln, falls mehr Zeilen enthalten
            if (z >= n) {
                // Zeilenanzahl erhoehen
                a = (double[]) resizeArray(a, n+1);
                n++; // Zeilenanzahl erhoehen
            }
            
            try {
                st = new StringTokenizer(line, useDelim);
                // Versuche m Spaltenwerte der Zeile z einzulesen
                a[z] = Double.parseDouble(st.nextToken());
            }
            catch (NumberFormatException e) {
                return null;
            }
            // Zeilenzaehler inkrementieren!
            z++;
        }
        // Wenn alles erfolgreich war, muss z mindestens 1 sein!
        if (z < 1) {
            return null;
        }
        // Datei schliessen
        br.close();
        return a;
    }
    
// ===============================================================
// ==== INTERNAL TOOLS SECTION ===================================
// ===============================================================
    /*
     * Diese Methode ver&auml;ndert die Gr&ouml;&szlig;e eines Arrays und
     * gibt das neudimensionierte Array zur&uuml;ck. Intern wird das Array
     * als Object (die allgemeinste Klasse in Java ) aufgefasst. In der
     * Anwendung muss der Rueckgabewert daher mit einer expliziten Typkonvertierung
     * auf den richtigen Array-Typ gesetzt werden.
     * 
     * @param oldArray Das zu &auml;ndernde Array.
     * @param newSize Die neue Gr&ouml;&szlig;e des Arrays.
     * @return Das neue Array als Objekt.
     */
    private Object resizeArray(Object oldArray, int newSize) {
        int oldSize = Array.getLength(oldArray);
        Class elementType = oldArray.getClass().getComponentType();
        Object newArray = Array.newInstance(elementType, newSize);
        int preserveLength = Math.min(oldSize, newSize);
        
        if (preserveLength > 0) {
            System.arraycopy(oldArray,0, newArray, 0, preserveLength);
        }
                
        if (newSize > oldSize) {
            try {
                int colSize = Array.getLength( Array.get(newArray, 0) );
                elementType = Array.get(newArray,0).getClass().getComponentType();
                for (int i = oldSize; i < newSize; i++) {
                    Array.set(newArray, i, Array.newInstance(elementType, colSize));
                }
            }
            catch (IllegalArgumentException e) {}
        }
        return newArray;
    }
    
}

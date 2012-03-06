/**********************************************
//
// DebugTerm.java
// @author Justin Salmon
// @version 
//
// <program description>
//
 *********************************************/


import java.awt.*;
import javax.swing.*;

class DebugTerm extends JApplet {

    private static JFrame debugWindow;
    private final JPanel debugPanel;
    private final JTextArea console;
    private final JScrollPane scroll;

    public DebugTerm() {

        debugWindow = new JFrame("Debug Window");
        debugPanel = new JPanel();
        console = new JTextArea();
        scroll = new JScrollPane(console,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scroll.getViewport().add(console);
        scroll.setPreferredSize(new Dimension(420, 260));

        debugWindow.add(debugPanel);
        debugPanel.add(scroll);

        debugWindow.setSize(450, 300);
        debugWindow.setLocation(780, 200);
        debugWindow.setFocusable(false);
        console.setEditable(false);

        debugWindow.setVisible(true);
    }

    public static void showDebug() {
        debugWindow.setVisible(true);
    }

    /**
     * Prints a string to the terminal with no newline
     */
    public void print(String s) {
        console.append(s);
        /* Autoscroll the slate */
        console.setCaretPosition(console.getDocument().getLength());
    }

    /**
     * Prints a string to the terminal including newline
     */
    public void println(String s) {
        print(s + "\n");
    }

    /**
     * Prints an entire packet to the terminal,
     * with no newline
     */
    public void print(char[] packet) {
        for (int i = 0; i < PendtableRecord.PACKETSIZE; i++) {
            putChar(packet[i]);
        }
    }

    /**
     * Prints an entire packet to the terminal,
     * including newline
     */
    public void println(char[] packet) {
        for (int i = 0; i < PendtableRecord.PACKETSIZE; i++) {
            putChar(packet[i]);
        }
    }

    /**
     * Prints a single character to the terminal
     */
    public void putChar(char ch) {
        StringBuilder str = new StringBuilder(" "); // a temp String!
        str.setCharAt(0, ch); // place the char in the String
        console.append(str.toString()); // display the char
        /* Autoscroll the slate */
        console.setCaretPosition(console.getDocument().getLength());
    }
}

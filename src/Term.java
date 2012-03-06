

/**
 * Term.java - generates a Frame window and sets up
 * the KeyListener interface for non-blocking keyboard input
 *
 * @author Justin
 */
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.*;

class Term extends JApplet {

    private final JFrame f;
    private final JPanel p;
    private final JMenuBar m;
    private final JMenu fileMenu;
    private final JMenu debugMenu;
    private final JMenuItem quitButton;
    private final JMenuItem debugButton;
    private final JTextArea slate;
    private final JTextField line;
    private final JScrollPane scroll;
    private char lastChar = 0;
    private boolean kbhit;

    /**
     * Constructor - Sets up the main terminal window and
     * action listeners
     */
    public Term() {

        f = new JFrame("Serial Instant Messenger");
        p = new JPanel(new BorderLayout(10, 10));
        m = new JMenuBar();
        fileMenu = new JMenu("File");
        debugMenu = new JMenu("Debug");
        quitButton = new JMenuItem("Quit");
        debugButton = new JMenuItem("Open Debug Console");
        slate = new JTextArea();
        line = new JTextField();
        scroll = new JScrollPane(slate,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scroll.getViewport().add(slate);
        scroll.setPreferredSize(new Dimension(520, 290));
        kbhit = false;

        f.setSize(530, 300);
        f.setLocation(250, 200);
        f.setVisible(true);
        f.setJMenuBar(m);

        f.add(p);
        m.add(fileMenu);
        m.add(debugMenu);
        fileMenu.add(quitButton);
        debugMenu.add(debugButton);
        p.add(scroll, BorderLayout.CENTER);
        p.add(line, BorderLayout.SOUTH);

        m.setBorder(BorderFactory.createEtchedBorder());
        p.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEmptyBorder(10, 5, 5, 5),
                "Serial Instant Messenger v0.1.5",
                TitledBorder.CENTER, TitledBorder.BELOW_TOP));
        slate.setBorder(BorderFactory.createEtchedBorder());
        line.setBorder(BorderFactory.createEtchedBorder());

        slate.setBackground(Color.white);
        slate.setForeground(Color.black);
        slate.setFont(new Font("Monospaced", Font.BOLD, 12));
        slate.setEditable(false);
        slate.setFocusable(false);

        f.addWindowListener(new WindowAdapter() {

            @Override
            public void windowClosing(WindowEvent e) {
                System.exit(0);
            }
        });

        line.addKeyListener(new KeyListener() {

            public void keyPressed(KeyEvent e) {
            }

            public void keyReleased(KeyEvent e) {
            }

            public void keyTyped(KeyEvent e) {
                lastChar = e.getKeyChar();
                kbhit = true;
            }
        });

        /* if quit button selected, exit program */
        quitButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                System.exit(0);
            }
        });

        debugButton.addActionListener(new ActionListener() {

            /* If the debug menu option selected,
             * pop out the debug window
             */
            public void actionPerformed(ActionEvent e) {
                DebugTerm.showDebug();
            }
        });

    }

    /**
     * Prints a string to the terminal with no newline
     */
    public void print(String s) {
        slate.append(s);
        /* Autoscroll the slate */
        slate.setCaretPosition(slate.getDocument().getLength());
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
     * Checks to see if a key was pressed
     */
    public boolean getkbhit() {
        return kbhit;
    }

    /**
     * Prints a single character to the terminal
     */
    public void putChar(char ch) {
        StringBuilder str = new StringBuilder(" "); // a temp String!
        str.setCharAt(0, ch); // place the char in the String
        slate.append(str.toString()); // display the char
        /* Autoscroll the slate */
        slate.setCaretPosition(slate.getDocument().getLength());
    }

    /**
     * Returns a single char to the caller
     */
    public char getChar() {
        if (kbhit == true) {
            kbhit = false;
            return lastChar;
        } else {
            return 0;
        }
    }

    /**
     * Deletes the current text from the input line, ready
     * for new text.
     */
    public void clearline() {
        line.setText(null);
    }
}

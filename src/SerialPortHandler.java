

/**
 * SerialPortHandler.java
 *
 * Sets up com port connection and deals with all
 * rxing and txing
 *
 * @author Justin Salmon
 */
import java.util.*;
import gnu.io.*;
import java.io.*;

class SerialPortHandler {

    private String PORT1, PORT2;
    private Enumeration portList;
    private CommPortIdentifier portId;
    private SerialPort serialPort = null;
    private OutputStream outputStream;
    private InputStream inputStream;
    public static String os;

    public SerialPortHandler() {

        /* Discover OS type so correct port names are used */
        serialcomm.dterminal.println("Checking your OS...");
        if (isWindows()) {
            os = "win";
            serialcomm.dterminal.println("OS: Windows");
            PORT1 = "COM1";
            PORT2 = "COM2";
        } else if (isMac()) {
            os = "mac";
            serialcomm.dterminal.println("OS: Mac");
            PORT1 = "tty0";
            PORT2 = "tty1"; 
        } else if (isUnix()) {
            os = "unix";
            serialcomm.dterminal.println("OS: Unix or Linux");
            PORT1 = "tty0";
            PORT2 = "tty1";
        } else {
            serialcomm.dterminal.println("Unsupported OS!");
        }

        /*
         * CommPortIdentifier is the comms port manager for controlling
         * access, ie. availability, and then open/closing.
         * getPortIdentifiers() returns an enumeration type
         */
        serialcomm.dterminal.println("Looking for ports...");
        portList = CommPortIdentifier.getPortIdentifiers();

        while (portList.hasMoreElements()) {

            /* enumeration refers to Objects - must be cast to required one */
            portId = (CommPortIdentifier) portList.nextElement();
            serialcomm.dterminal.print("" + portId.getName());

            if (portId.getPortType() == CommPortIdentifier.PORT_SERIAL) {

                if (portId.getName().equals(PORT1)) { // check for the rqd com port
                    try {
                        /* open using "java lan" as application and 1sec blocking time */
                        serialPort = (SerialPort) portId.open("java lan", 2000);
                        serialPort.setSerialPortParams(9600,
                                SerialPort.DATABITS_8,
                                SerialPort.STOPBITS_1,
                                SerialPort.PARITY_NONE);

                        /* stop blocking, immediate return */
                        serialPort.enableReceiveThreshold(0);

                        /* flow control- RTS/CTS */
                        serialPort.setRTS(true);
                        serialPort.setDTR(true);

                        /* set data streams */
                        outputStream = serialPort.getOutputStream();
                        inputStream = serialPort.getInputStream();
                        serialcomm.dterminal.print(" port opened!");
                    } catch (IOException e) {
                    } catch (UnsupportedCommOperationException e) {
                        serialcomm.dterminal.println("Unsupported use...");
                        System.exit(0);
                    } catch (PortInUseException e) {
                        serialcomm.dterminal.println(" port in use...");
                        PORT1 = PORT2;
                    }
                }
            }
            serialcomm.dterminal.println("");
        }
        serialcomm.dterminal.println("Finished checking!");
        if (serialPort == null) {
            serialcomm.dterminal.println("No " + PORT1 + " port found...");
            System.exit(0);
        }

    }

    /**
     * Gets a single char from the current serial input stream.
     */
    public char getChar() {
        char b = (char) -1;
        try {
            b = (char) inputStream.read();
        } catch (IOException e) {
        }
        return b;
    }

    /**
     * Places a single char to the serial output stream.
     * @param b
     */
    public void putChar(char b) {
        try {
            outputStream.write(b);
        } catch (IOException e) {
        }
    }

    /**
     * Sends a char array one char at a time to the serial port
     * @param ppacket
     */
    public int sendpacket(char[] ppacket) {
        for (int i = 0; i < ppacket.length; i++) {
            putChar(ppacket[i]);
        }
        serialcomm.dterminal.print("Packet sent: ");
        serialcomm.dterminal.println(ppacket);
        /* failure handled in above exception */
        return 0;
    }

    /**
     * OS detecting methods
     */
    public static boolean isWindows() {
        String o = System.getProperty("os.name").toLowerCase();
        return (o.indexOf("win") >= 0);
    }

    public static boolean isMac() {
        String o = System.getProperty("os.name").toLowerCase();
        return (o.indexOf("mac") >= 0);
    }

    public static boolean isUnix() {
        String o = System.getProperty("os.name").toLowerCase();
        return (o.indexOf("nix") >= 0 || o.indexOf("nux") >= 0);
    }
}

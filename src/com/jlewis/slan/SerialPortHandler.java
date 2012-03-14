package com.jlewis.slan;

import java.util.*;
import gnu.io.*;
import java.io.*;

/**
 * SerialLAN.java
 * 
 * @author Justin Lewis Salmon
 * @student_id 10000937
 * 
 *             Represents a citation between papers where the endpoint of the
 *             connection is the paper that is making the reference, i.e. the
 *             paper at the end of this connection is referencing me, so i'm
 *             going to store it as a citation.
 */
class SerialPortHandler {

	private String PORT1, PORT2;
	private Enumeration<CommPortIdentifier> portList;
	private CommPortIdentifier portId;
	private SerialPort serialPort = null;
	private OutputStream outputStream;
	private InputStream inputStream;
	public static String os;

	public SerialPortHandler() {

		/* Discover OS type so correct port names are used */
		SerialLAN.dterminal.println("Checking your OS...");
		if (isWindows()) {
			os = "win";
			SerialLAN.dterminal.println("OS: Windows");
			PORT1 = "COM1";
			PORT2 = "COM2";
		} else if (isUnix()) {
			os = "unix";
			SerialLAN.dterminal.println("OS: Unix or Linux");
			PORT1 = "/dev/ttyS60";
			PORT2 = "/dev/ttyS50";
		} else {
			SerialLAN.dterminal.println("Unsupported OS!");
		}

		/*
		 * CommPortIdentifier is the comms port manager for controlling access,
		 * ie. availability, and then open/closing. getPortIdentifiers() returns
		 * an enumeration type
		 */
		SerialLAN.dterminal.println("Looking for ports...");
		portList = CommPortIdentifier.getPortIdentifiers();

		while (portList.hasMoreElements()) {

			/* enumeration refers to Objects - must be cast to required one */
			portId = portList.nextElement();
			SerialLAN.dterminal.print("" + portId.getName());

			if (portId.getPortType() == CommPortIdentifier.PORT_SERIAL) {

				/* check for the rqd com port */
				if (portId.getName().equals(PORT1)) {
					try {
						/*
						 * open using "java lan" as application and 1sec
						 * blocking time
						 */
						serialPort = (SerialPort) portId.open("java lan", 2000);
						serialPort.setSerialPortParams(9600,
								SerialPort.DATABITS_8, SerialPort.STOPBITS_1,
								SerialPort.PARITY_NONE);

						/* stop blocking, immediate return */
						serialPort.enableReceiveThreshold(0);

						/* flow control- RTS/CTS */
						serialPort.setRTS(true);
						serialPort.setDTR(true);

						/* set data streams */
						outputStream = serialPort.getOutputStream();
						inputStream = serialPort.getInputStream();
						SerialLAN.dterminal.print(" port opened!");
					} catch (IOException e) {
					} catch (UnsupportedCommOperationException e) {
						SerialLAN.dterminal.println("Unsupported use...");
						System.exit(0);
					} catch (PortInUseException e) {
						SerialLAN.dterminal.println(" port in use...");
						PORT1 = PORT2;
					}
				}
			}
			SerialLAN.dterminal.println("");
		}
		SerialLAN.dterminal.println("Finished checking!");
		if (serialPort == null) {
			SerialLAN.dterminal.println("No " + PORT1 + " port found...");
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
	 * 
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
	 * 
	 * @param ppacket
	 */
	public int sendpacket(char[] ppacket) {
		for (int i = 0; i < ppacket.length; i++) {
			putChar(ppacket[i]);
		}
		SerialLAN.dterminal.print("Packet sent: ");
		SerialLAN.dterminal.println(ppacket);
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

	public static boolean isUnix() {
		String o = System.getProperty("os.name").toLowerCase();
		return (o.indexOf("nix") >= 0 || o.indexOf("nux") >= 0);
	}
}

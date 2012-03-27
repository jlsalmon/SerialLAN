package com.jlewis.slan;

import java.util.Random;

/**
 * SerialLAN.java
 * 
 * @author Justin Lewis Salmon 10000937
 * @author Mokdeep Sandhu 10029172
 * 
 *         Main controller class, everything happens here. There are too many
 *         static variables, and the whole thing is written fairly poorly, but
 *         it works.
 */
public class SerialLAN {

	private static SerialPortHandler sio;
	private static Term term;
	public static DebugTerm dterm;
	/* Keyboard user input state index */
	private static int userInputState;
	/* Keyboard states */
	private static final int LOGIN = 0, ADDRESS_PENDING = 1,
			MENU = 2, GET_ADDRESS = 3, INPUT_MESSAGE = 4, LOGOUT = 5;
	/* The char the user types in the terminal */
	private static char key = 0;

	/* Receiver states */
	private static final int WAITING = 0, RECEIVING = 1, ARRIVED = 2,
			DECODING = 3;
	/* The reciever state index */
	private static int receiverState = WAITING;
	/* Index into PendTable */
	private static int pendTableIndex = 0;
	private static final int PENDTABLE_SIZE = 26;
	/* An array of PendtableRecords = the actual pendtable */
	private static PendtableRecord[] pendtable =
			new PendtableRecord[PENDTABLE_SIZE];

	/* My login ID */
	private static char myaddr = 0;
	/* Debug flag */
	public static boolean debug = true;

	/* Global packet arrays */
	private static final int PACKETSIZE = 16;
	private static final int PAYLOAD_SIZE = 9;
	private static int payloadBuffer = 0;
	private static char kbdpacket[] = new char[PACKETSIZE];
	private static char rxpacket[] = new char[PACKETSIZE];
	private static char txpacket[] = new char[PACKETSIZE];
	/* Positions of special characters in a packet */
	private static final int SOM = 0, DEST = 1, SRC = 2, TYPE = 3,
			CHKSM = 14, EOM = 15;

	private static int showMenu;
	private static char unique;
	private static int checksum;

	/**
	 * Main entry point. Starts userIO/rx/tx superloop.
	 */
	public static void main(String[] args) {

		/* Create term window for keyboard input */
		term = new Term();
		/* Create debug terminal, hide initially */
		dterm = new DebugTerm();
		/* Open the serial port */
		sio = new SerialPortHandler();
		/* Initialise local packets */
		kbdpacket = clearpacket(kbdpacket);
		rxpacket = clearpacket(rxpacket);
		txpacket = clearpacket(txpacket);

		/*
		 * User just started the program, so logging in will be the first task
		 */
		term.print("To login, key in a character (A-Z): ");
		userInputState = LOGIN;

		/* Initialise the pendtable */
		for (int i = 0; i < PENDTABLE_SIZE; i++) {
			pendtable[i] = new PendtableRecord();
		}

		/* Superloop, here we go */
		while (true) {
			userIO();
			transmitter();
			receiver();
		}

	}

	/**
	 * Main non-blocking user input/output task.
	 */
	private static void userIO() {

		switch (userInputState) {
		case LOGIN:
			/* Check for key typed */
			if (term.getkbhit()) {
				key = Character.toUpperCase(term.getChar());
				/* Check ID is valid */
				if (key >= 'A' && key <= 'Z') {
					term.putChar(key);
					term.putChar('\n');
					if (pendtable[key - 'A'].getLoggedin() == 1) {
						term.print("Node ID in use. Try another: ");
						term.clearline();
						break;
					} else {
						login();
					}
					/* Move to next state */
					userInputState = ADDRESS_PENDING;
				} else {
					term.print("\nInvalid input char. [A-Z]: ");
					term.clearline();
					break;
				}
			}
			break;
		case ADDRESS_PENDING:
			/**
			 * Waiting for the return of transmitted Login message if login
			 * response okay, set values, next userState
			 */
			if (pendtable[myaddr - 'A'].getPending() == 0
					&& pendtable[myaddr - 'A'].getLoggedin() == 0) {
				term.print("Seems we're all alone...");
				userInputState = LOGIN;
			}
			if (pendtable[myaddr - 'A'].getLoggedin() == -1) {
				term.print("Your node id is now set to: " + myaddr);
				term.clearline();
				/* Move to next state */
				userInputState = MENU;
				showMenu = 1;
			}
			break;
		case MENU:
			/* Display menu options */
			if (showMenu == 1) {
				showMenu = 0;
				term
						.print("\nOptions: (D)estination, (S)end, (C)ancel, (L)ogout");
			}
			/* Poll for instruction char */
			if (term.getkbhit()) {
				key = Character.toUpperCase(term.getChar());
				term.clearline();
				switch (key) {
				case 'D':
					/* Selecting a destination */
					term.print("\nEnter destination node ID [A-Z]: ");
					/* moving on */
					userInputState = GET_ADDRESS;
					break;
				case 'S':
					/* Sending the data packet */
					if (kbdpacket[CHKSM] != ' ') {
						/* enter record in pendtable for sending */
						sio.sendpacket(kbdpacket);
						pendtable[kbdpacket[DEST] - 'A']
								.setPacket(kbdpacket);
						pendtable[kbdpacket[DEST] - 'A']
								.setPending(4);
						pendtable[kbdpacket[DEST] - 'A']
								.setDelay(PendtableRecord.DELAY);
						term.print("\nMessage sent!");
						term.clearline();
						/* Clear the packet for next use! */
						kbdpacket = clearpacket(kbdpacket);
					} else {
						term
								.print("\nNo message to send, or already sent.");
						term.clearline();
					}
					/* Back to menu */

					break;
				case 'C':
					/* Cancel sending of packet */
					if (kbdpacket[CHKSM] != ' ') {
						kbdpacket = clearpacket(kbdpacket);
						term
								.print("\nMessage cancelled. Packet cleared.");
						term.clearline();
					} else {
						term.print("\nNothing to do...");
						term.clearline();
					}
					/* Back to menu */
					userInputState = MENU;
					showMenu = 1;
					break;
				case 'L':
					/* Take user to logout state */
					term
							.print("\nAre you sure you want to log out? Y/N: ");
					term.clearline();
					userInputState = LOGOUT;
					break;
				default:
					term
							.print("\nInvalid choice. (D)est, (S)end, (C)ancel or (L)ogout: ");
					term.clearline();
					break;
				}
			}
			break;
		case GET_ADDRESS:
			if (term.getkbhit()) {
				key = Character.toUpperCase(term.getChar());
				term.putChar(key);
				/* Check node is valid */
				if (key >= 'A' && key <= 'Z') {
					if (pendtable[key - 'A'].getLoggedin() != 0) {
						/* Build packet header */
						kbdpacket = clearpacket(kbdpacket);
						kbdpacket[SRC] = myaddr;
						kbdpacket[DEST] = key;
						kbdpacket[TYPE] = 'D';
						/* moving on */
						term
								.print("\nEnter message (10 chars max): ");
						term.clearline();
						userInputState = INPUT_MESSAGE;
					} else {
						term
								.print("\nNode not logged in. Choose another. [A-Z]: ");
						term.clearline();
					}
				} else {
					term.print("\nInvalid node. [A-Z]: ");
					term.clearline();
				}
			}
			break;
		case INPUT_MESSAGE:
			if (term.getkbhit()) {
				key = term.getChar();

				if (key == '\n') {
					payloadBuffer = 0;
					setchsum(kbdpacket);
					term.print("\nMessage finished. Packet ready.");
					term.clearline();
					/* move on */
					userInputState = MENU;
					showMenu = 1;
					break;
				}

				if (payloadBuffer < PAYLOAD_SIZE) {
					/* check for backspace key */
					if (key == '\b' && payloadBuffer != 0) {
						kbdpacket[--payloadBuffer + 4] = ' ';
					} else {
						/* build payload */
						kbdpacket[payloadBuffer + 4] = key;
						payloadBuffer++;
					}
				} else {
					if (key == '\b') {
						payloadBuffer--;
						break;
					}
					term
							.print("\n\nMessage limit reached. Packet ready.");
					term.clearline();
					kbdpacket[payloadBuffer + 4] = key;
					payloadBuffer = 0;
					setchsum(kbdpacket);
					/* move on */
					userInputState = MENU;
					showMenu = 1;
					break;
				}
			}
			break;
		case LOGOUT:
			if (term.getkbhit()) {
				key = Character.toUpperCase(term.getChar());
				term.putChar(key);
				if (key == 'Y') {
					logout();
					term.clearline();
					userInputState = LOGIN;

				} else if (key == 'N') {
					/* Logout aborted */
					term.println("\nLogout aborted.");
					term.clearline();
					userInputState = MENU;
					showMenu = 1;
				} else {
					/* Invalid */
					term.print("\nInvalid choice. [Y/N]: ");
					term.clearline();
				}
			}
			break;
		default: /* should never get here */
			dterm.println("\nError in keyboard user state machine!!");
			break;
		}
	}

	/**
	 * Main receiver task.
	 */
	private static void receiver() {

		switch (receiverState) {
		case WAITING:
			/* Poll port for '{' */
			if ((sio.getChar()) == '{') {
				rxpacket[0] = '{';
				receiverState = RECEIVING;
			}
			break;
		case RECEIVING:
			for (int i = 1; i < PACKETSIZE; i++) {
				rxpacket[i] = sio.getChar();
			}

			if (rxpacket[EOM] == '}') {
				receiverState = ARRIVED;
			} else {
				dterm.print("\nrx packet error...");
				receiverState = WAITING;
			}
			break;
		case ARRIVED:
			/* check sum */
			dterm.print("\nPacket received: ");
			dterm.println(rxpacket);
			if (chksm(rxpacket) == (int) rxpacket[CHKSM]) {
				receiverState = DECODING;
			} else {
				if (myaddr != 0) {
					nak(rxpacket);
				}
				dterm.println("\nBad checksum detected!");
				receiverState = DECODING;
			}
			break;
		case DECODING:
			if (myaddr == 0) {
				sio.sendpacket(rxpacket);
			} else if (rxpacket[1] == myaddr) {
				if (rxpacket[2] == myaddr) {
					/* to me from me */
					switch (rxpacket[3]) {
					case 'L':
						if (rxpacket[4] == unique) {
							/* my login id is OK, del packet */
							rxpacket = clearpacket(rxpacket);
							pendtable[myaddr - 'A'].setPending(0);
							pendtable[myaddr - 'A'].clearPacket();
							pendtable[myaddr - 'A'].setLoggedin(-1);
							term.println("\nLogged in successfully.");
						} else {
							term
									.print("\nLogin conflict detected! Try another ID: ");
							term.clearline();
							userInputState = LOGIN;
						}
						break;
					case 'R':
						/* illegal */
						rxpacket = clearpacket(rxpacket);
						break;
					case 'D':
						/* test message, del packet, return ACK */
						term.println("\nTest message received.");
						term.println(rxpacket);
						ack(rxpacket);
						pendtable[myaddr - 'A'].clearPacket();
						rxpacket = clearpacket(rxpacket);
						break;
					case 'A':
						/* cancel pending entry, delete packet */
						pendtable[myaddr - 'A'].setPending(0);
						pendtable[myaddr - 'A'].clearPacket();
						rxpacket = clearpacket(rxpacket);
						dterm.print("\nPacket acknowledged.");
						break;
					case 'N':
						/* retransmit pending table entry */
						pendtable[myaddr - 'A'].setPacket(rxpacket);
						pendtable[myaddr - 'A'].setPending(5);
						pendtable[myaddr - 'A']
								.setDelay(PendtableRecord.DELAY);
						dterm.print("\nBad packet received!");
						rxpacket = clearpacket(rxpacket);
						break;
					case 'X':
						/* logout now, del myaddr & pending table */
						pendtable[myaddr - 'A'].setLoggedin(0);
						pendtable[myaddr - 'A'].clearPacket();
						myaddr = 0;
						term.println("Logged out successfully.\n");
						term
								.print("To login, key in a character (A-Z): ");
						rxpacket = clearpacket(rxpacket);
						userInputState = LOGIN;
						break;
					default:
						break;
					}
				} else {
					/* to me from you */
					switch (rxpacket[3]) {
					case 'L':
						/* illegal */
						rxpacket = clearpacket(rxpacket);
						break;
					case 'R':
						/* response to my login, del packet, update pendtable */
						pendtable[rxpacket[SRC] - 'A'].setLoggedin(1);
						rxpacket = clearpacket(rxpacket);
						break;
					case 'D':
						/* real message. return ACK, del packet */
						printPayload(rxpacket);
						ack(rxpacket);
						rxpacket = clearpacket(rxpacket);
						break;
					case 'A':
						/* cancel pending entry, delete packet */
						pendtable[rxpacket[SRC] - 'A'].setPending(0);
						pendtable[rxpacket[SRC] - 'A'].clearPacket();
						rxpacket = clearpacket(rxpacket);
						dterm.print("\nAcknowledgement received.");
						userInputState = MENU;
						showMenu = 1;
						break;
					case 'N':
						/* retransmit pending table entry */
						pendtable[rxpacket[SRC] - 'A'].setPending(5);
						pendtable[rxpacket[SRC] - 'A']
								.setDelay(PendtableRecord.DELAY);
						break;
					case 'X':
						/* illegal */
						rxpacket = clearpacket(rxpacket);
						break;
					default:
						break;
					}
				}
			} else {
				if (rxpacket[2] == myaddr) {
					/* to you from me */
					switch (rxpacket[3]) {
					case 'L':
						/* illegal */
						rxpacket = clearpacket(rxpacket);
						break;
					case 'R':
						/* error, where has he gone now? */
						pendtable[rxpacket[DEST] - 'A']
								.setLoggedin(0);
						rxpacket = clearpacket(rxpacket);
						break;
					case 'D':
						/* rx failed, del packet, re-tx from pendtable */
						pendtable[rxpacket[DEST] - 'A'].setPending(5);
						pendtable[rxpacket[DEST] - 'A']
								.setDelay(PendtableRecord.DELAY);
						rxpacket = clearpacket(rxpacket);
						break;
					case 'A':
						/* rx failed, del packet */
						rxpacket = clearpacket(rxpacket);
						break;
					case 'N':
						/* rx failed, del packet */
						rxpacket = clearpacket(rxpacket);
						break;
					case 'X':
						/* illegal */
						rxpacket = clearpacket(rxpacket);
						break;
					default:
						break;
					}
				} else {
					/* to you from you */
					switch (rxpacket[3]) {
					case 'L':
						/* re-tx packet, update pending, then send R */
						if (myaddr != 0) {
							sio.sendpacket(rxpacket);
							pendtable[rxpacket[SRC] - 'A']
									.setLoggedin(1);
							loginReply(rxpacket[SRC]);
						} else {
							pendtable[rxpacket[DEST] - 'A']
									.setPacket(rxpacket);
							pendtable[rxpacket[SRC] - 'A']
									.setLoggedin(1);
						}
						break;
					case 'R':
						/* illegal */
						sio.sendpacket(rxpacket);
						break;
					case 'D':
						/* re-tx packet */
						sio.sendpacket(rxpacket);
						break;
					case 'A':
						/* re-tx packet */
						sio.sendpacket(rxpacket);
						break;
					case 'N':
						/* re-tx packet */
						sio.sendpacket(rxpacket);
						break;
					case 'X':
						/* re-tx packet, amend pendtable */
						sio.sendpacket(rxpacket);
						pendtable[rxpacket[SRC] - 'A'].setLoggedin(0);
						break;
					default:
						break;

					}
				}
			}
			receiverState = WAITING;
			break;
		/* End of decoding block */
		default:
			dterm.print("\nError in reciever state machine.");
			break;
		}

	}

	/**
	 * transmitter() - Actually sends stuff!
	 */
	private static void transmitter() {

		/* inc index and check limit */
		if (++pendTableIndex > ('Z' - 'A')) {
			pendTableIndex = 0;
		}
		/* check if message to send */
		if (pendtable[pendTableIndex].getPending() > 0) {
			/* Start of countdown? */
			if (pendtable[pendTableIndex].getDelay() == PendtableRecord.DELAY) {
				/* actually send the packet on this attempt */
				sio.sendpacket(pendtable[pendTableIndex].getPacket());
			}
			pendtable[pendTableIndex].decDelay();

			if (pendtable[pendTableIndex].getDelay() == 0) {
				/* Countdown finished */
				pendtable[pendTableIndex].decPending();
				/* any attempts left? */
				if (pendtable[pendTableIndex].getPending() > 0) {
					/* Reset countdown */
					pendtable[pendTableIndex]
							.setDelay(PendtableRecord.DELAY);
				}
			}
		}
	}

	/**
	 * Re-iniialises a packet to a reusable state.
	 */
	public static char[] clearpacket(char[] packet) {
		char[] p = new char[PACKETSIZE];
		/* Clear packet to spaces */
		for (int i = 0; i < PACKETSIZE; i++) {
			packet[i] = ' ';
			p[i] = ' ';
		}
		/* Set SOT and EOT */
		p[SOM] = '{';
		packet[SOM] = '{';
		p[EOM] = '}';
		packet[EOM] = '}';
		return p;
	}

	/**
	 * Calculates the checksum for a packet and adds it to the packet array
	 * index
	 */
	public static void setchsum(char[] packet) {
		int sum = 0;
		packet[CHKSM] = 0;
		for (int i = 0; i < PACKETSIZE; i++) {
			if (i != 14) {
				sum += packet[i];
			}
		}
		/* inverse mod 128-bit sum */
		checksum = (sum % 128);
		dterm.println("\nChecksum set as: " + checksum);
		packet[CHKSM] = (char) checksum;
	}

	/**
	 * Decodes the checksum for a packet.
	 */
	private static int chksm(char[] rxpacket) {
		int cs = 0;
		for (int i = 0; i < PACKETSIZE; i++) {
			if (i == 14) {
				continue;
			}
			cs += rxpacket[i];
		}
		cs = (cs % 128);
		dterm.println("\nDecoded checksum: " + cs);
		return cs;
	}

	/**
	 * ack - Sends an acknowledgement packet to sender
	 */
	private static void ack(char[] rxpacket) {
		/* Build ack packet */
		txpacket = clearpacket(txpacket);
		txpacket[DEST] = rxpacket[SRC];
		txpacket[SRC] = myaddr;
		txpacket[TYPE] = 'A';
		setchsum(txpacket);
		sio.sendpacket(txpacket);
		dterm.println("\nPacket acknowledged.");
	}

	/**
	 * Sends a nak packet after receiving a bad packet
	 */
	private static void nak(char[] rxpacket) {
		/* Build nak packet */
		txpacket = clearpacket(txpacket);
		txpacket[DEST] = rxpacket[SRC];
		txpacket[SRC] = myaddr;
		txpacket[TYPE] = 'N';
		setchsum(txpacket);
		/* Send the packet once */
		pendtable[key - 'A'].setPacket(txpacket);
		pendtable[key - 'A'].setPending(1);
		dterm.println("\nPacket not acknowledged...");
	}

	/**
	 * Logs in this user.
	 */
	private static void login() {
		term.println("Logging in...");
		/* save my login id to check against response from Lan */
		myaddr = key;
		/* Clean and build a fresh packet */
		kbdpacket = clearpacket(kbdpacket);
		kbdpacket[DEST] = myaddr;
		kbdpacket[SRC] = myaddr;
		kbdpacket[TYPE] = 'L'; /* a login packet */
		unique = (char) (new Random().nextInt(255));
		kbdpacket[4] = unique;
		setchsum(kbdpacket);
		/* Put the login packet in my row of the pendtable */
		pendtable[myaddr - 'A'].setPacket(kbdpacket);
		/* Try to login 5 times, with delay. */
		sio.sendpacket(kbdpacket);
		pendtable[myaddr - 'A'].setPending(4);
		pendtable[myaddr - 'A'].setDelay(PendtableRecord.DELAY);
		term.println("Waiting for another node to respond...");
	}

	/**
	 * Logs this user out..
	 */
	private static void logout() {
		/* Logout confirmation */
		txpacket = clearpacket(txpacket);
		/* Build a logout packet */
		txpacket[DEST] = myaddr;
		txpacket[SRC] = myaddr;
		txpacket[TYPE] = 'X';
		setchsum(txpacket);
		/* Pend the logout packet for 5 tries */
		sio.sendpacket(txpacket);
		pendtable[myaddr - 'A'].setPacket(txpacket);
		pendtable[myaddr - 'A'].setPending(4);
		pendtable[myaddr - 'A'].setDelay(PendtableRecord.DELAY);
		term.println("\nLogging out...");
	}

	/**
	 * Sends a login reply packet when a new node logs in.
	 */
	private static void loginReply(char source) {
		dterm.println("\nReplying to login from " + source + "...");
		/* Build login reply packet */
		txpacket = clearpacket(txpacket);
		txpacket[DEST] = source;
		txpacket[SRC] = myaddr;
		txpacket[TYPE] = 'R';
		setchsum(txpacket);
		sio.sendpacket(txpacket);
	}

	/**
	 * Prints indexes 4 to 14 to the terminal
	 */
	private static void printPayload(char[] rxpacket) {
		String s = "";
		term.print("\n" + rxpacket[SRC] + " says > ");
		for (int i = 4; i < PendtableRecord.PACKETSIZE - 2; i++) {
			s += rxpacket[i];
		}
		/* Stripping excess whitespace */
		s = s.replaceAll("\\s+", " ");
		term.println(s);
	}
}

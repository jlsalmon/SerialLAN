

/**
 * PendtableRecord.java
 *
 * todo: write description
 *
 * @author Justin
 */

public class PendtableRecord {

    public static final int PACKETSIZE = 16;
    /* Initial pendtable values for this user */
    private int loggedin = 0,
            pending = 0,
            delay = 0;

    private char[] packet = new char[PACKETSIZE];
    public static final int DELAY = 5000;

    /**
     * Constructor - sets all pendtable columns.
     */
    public PendtableRecord() {
        loggedin = 0;
        pending = 0;
        delay = 0;
        clearPacket();
    }

    public void setLoggedin(int logIn) {
        loggedin = logIn;
    }

    public int getLoggedin() {
        return loggedin;
    }

    public void setPending(int pendValue) {
        pending = pendValue;
    }

    public int getPending() {
        return pending;
    }

    public int decPending() {
        pending--;
        return pending;
    }

    public void setDelay(int delayValue) {
        delay = delayValue;
    }

    public int getDelay() {
        return delay;
    }

    public int decDelay() {
        delay--;
        return delay;
    }

    public final void clearPacket() {
        for (int i = 0; i < packet.length; i++) {
            packet[i] = ' ';
        }
    }

    public void setPacket(char[] newPacket) {
        for (int i = 0; i < packet.length; i++) {
            packet[i] = newPacket[i];
        }
    }

    public char[] getPacket() {
        return packet;
    }
} //PendtableRecord class


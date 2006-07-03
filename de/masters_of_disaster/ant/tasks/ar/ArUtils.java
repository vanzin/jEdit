package de.masters_of_disaster.ant.tasks.ar;

/**
 * This class provides static utility methods to work with byte streams.
 */
public class ArUtils {
    /**
     * Parse an octal string from a header buffer. This is used for the
     * file permission mode value.
     *
     * @param header The header buffer from which to parse.
     * @param offset The offset into the buffer from which to parse.
     * @param length The number of header bytes to parse.
     * @return The long value of the octal string.
     */
    public static long parseOctal(byte[] header, int offset, int length) {
        long    result = 0;
        int     end = offset + length;

        for (int i=offset ; i<end ; i++) {
            if (header[i] == (byte) ' ') {
                break;
            }
            result = (result << 3) + (header[i] - '0');
        }

        return result;
    }

    /**
     * Parse an entry name from a header buffer.
     *
     * @param header The header buffer from which to parse.
     * @param offset The offset into the buffer from which to parse.
     * @param length The number of header bytes to parse.
     * @return The header's entry name.
     */
    public static StringBuffer parseName(byte[] header, int offset, int length) {
        StringBuffer result = new StringBuffer(length);
        int          end = offset + length;

        for (int i=offset ; i<end ; i++) {
            if (header[i] == ' ') {
                break;
            }

            result.append((char) header[i]);
        }

        return result;
    }

    /**
     * Write a name into a byte array.
     *
     * @param name The name to write.
     * @param buf The byte array into which to write.
     * @param offset The offset into the buffer from which to write.
     * @param length The number of header bytes to write.
     * @return The number of bytes written to the buffer.
     */
    public static int getNameBytes(StringBuffer name, byte[] buf, int offset, int length) {
        int i;
        int c = name.length();

        for (i=0 ; i<length && i<c ; i++) {
            buf[offset+i] = (byte) name.charAt(i);
        }

        while (i<length) {
            buf[offset+i] = (byte) ' ';
            i++;
        }

        return offset + length;
    }

    /**
     * Write a long value into a byte array.
     *
     * @param value The value to write.
     * @param buf The byte array into which to write.
     * @param offset The offset into the buffer from which to write.
     * @param length The number of header bytes to write.
     * @return The number of bytes written to the buffer.
     */
    public static int getLongBytes(long value, byte[] buf, int offset, int length) {
        int i;
        String tmp = Long.toString(value);
        int c = tmp.length();

        for (i=0 ; i<length && i<c ; i++) {
            buf[offset+i] = (byte) tmp.charAt(i);
        }

        while (i<length) {
            buf[offset+i] = (byte) ' ';
            i++;
        }

        return offset + length;
    }

    /**
     * Write an int value into a byte array.
     *
     * @param value The value to write.
     * @param buf The byte array into which to write.
     * @param offset The offset into the buffer from which to write.
     * @param length The number of header bytes to write.
     * @return The number of bytes written to the buffer.
     */
    public static int getIntegerBytes(int value, byte[] buf, int offset, int length) {
        int i;
        String tmp = Integer.toString(value);
        int c = tmp.length();

        for (i=0 ; i<length && i<c ; i++) {
            buf[offset+i] = (byte) tmp.charAt(i);
        }

        while (i<length) {
            buf[offset+i] = (byte) ' ';
            i++;
        }

        return offset + length;
    }

    /**
     * Write an octal value into a byte array.
     *
     * @param value The value to write.
     * @param buf The byte array into which to write.
     * @param offset The offset into the buffer from which to write.
     * @param length The number of header bytes to write.
     * @return The number of bytes written to the buffer.
     */
    public static int getOctalBytes(long value, byte[] buf, int offset, int length) {
        int i;
        String tmp = Long.toOctalString(value);
        int c = tmp.length();

        for (i=0 ; i<length && i<c ; i++) {
            buf[offset+i] = (byte) tmp.charAt(i);
        }

        while (i<length) {
            buf[offset+i] = (byte) ' ';
            i++;
        }

        return offset + length;
    }
}

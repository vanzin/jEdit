package de.masters_of_disaster.ant.tasks.ar;

import java.io.FilterOutputStream;
import java.io.OutputStream;
import java.io.IOException;

/**
 * The ArOutputStream writes an ar archive as an OutputStream.
 * Methods are provided to put entries, and then write their contents
 * by writing to this stream using write().
 */
public class ArOutputStream extends FilterOutputStream {
    /** Fail if a long file name is required in the archive or the name contains spaces. */
    public static final int LONGFILE_ERROR = 0;

    /** Long paths will be truncated in the archive. Spaces are replaced by '_' */
    public static final int LONGFILE_TRUNCATE = 1;

    /** GNU ar variant is used to store long file names and file names with spaced in the archive. */
    public static final int LONGFILE_GNU = 2;

    /** BSD ar variant is used to store long file names and file names with spaced in the archive. */
    public static final int LONGFILE_BSD = 3;

    protected int       currSize;
    protected int       currBytes;
    protected byte[]    oneBuf;
    protected int       longFileMode = LONGFILE_ERROR;
    protected boolean   writingStarted = false;
    protected boolean   inEntry = false;

    public ArOutputStream(OutputStream os) throws IOException {
        super(os);
    	if (null == os) {
    	    throw new NullPointerException("os must not be null");
    	}
        this.out.write(ArConstants.ARMAGIC,0,ArConstants.ARMAGIC.length);
        this.oneBuf = new byte[1];
    }

    public void setLongFileMode(int longFileMode) {
    	if (writingStarted) {
    	    throw new IllegalStateException("longFileMode cannot be changed after writing to the archive has begun");
    	}
        if (LONGFILE_GNU == longFileMode) {
            throw new UnsupportedOperationException("GNU variant isn't implemented yet");
        }
        if (LONGFILE_BSD == longFileMode) {
            throw new UnsupportedOperationException("BSD variant isn't implemented yet");
        }
        this.longFileMode = longFileMode;
    }

    /**
     * Put an entry on the output stream. This writes the entry's
     * header record and positions the output stream for writing
     * the contents of the entry. Once this method is called, the
     * stream is ready for calls to write() to write the entry's
     * contents. Once the contents are written, closeEntry()
     * <B>MUST</B> be called to ensure that all buffered data
     * is completely written to the output stream.
     *
     * @param entry The ArEntry to be written to the archive.
     */
    public void putNextEntry(ArEntry entry) throws IOException {
    	writingStarted = true;
    	if (inEntry) {
    	    throw new IOException("the current entry has to be closed before starting a new one");
    	}
        String filename = entry.getFilename();
        if ((filename.length() >= ArConstants.NAMELEN)
              && (longFileMode != LONGFILE_TRUNCATE)) {
            throw new RuntimeException("file name \"" + entry.getFilename()
                                         + "\" is too long ( > "
                                         + ArConstants.NAMELEN + " bytes )");
        }
        if (-1 != filename.indexOf(' ')) {
            if (longFileMode == LONGFILE_TRUNCATE) {
                entry.setFilename(filename.replace(' ','_'));
            } else {
                throw new RuntimeException("file name \"" + entry.getFilename()
                                             + "\" contains spaces");
            }
        }

        byte[] headerBuf = new byte[ArConstants.HEADERLENGTH];
        entry.writeEntryHeader(headerBuf);
        this.out.write(headerBuf,0,ArConstants.HEADERLENGTH);

        this.currBytes = 0;
        this.currSize = (int) entry.getSize();
        inEntry = true;
    }

    /**
     * Close an entry. This method MUST be called for all file
     * entries that contain data. The reason is that we must
     * pad an entries data if it is of odd size.
     */
    public void closeEntry() throws IOException {
        if (!inEntry) {
            throw new IOException("we are not in an entry currently");
        }

        if (this.currBytes < this.currSize) {
            throw new IOException("entry closed at '" + this.currBytes
                                  + "' before the '" + this.currSize
                                  + "' bytes specified in the header were written");
        }

        if (1 == (this.currSize & 1)) {
            this.out.write(ArConstants.PADDING,0,1);
        }

        inEntry = false;
    }

    /**
     * Writes a byte to the current ar archive entry.
     *
     * This method simply calls write( byte[], int, int ).
     *
     * @param b The byte to write to the archive.
     */
    public void write(int b) throws IOException {
        this.oneBuf[0] = (byte) b;
        this.write(this.oneBuf, 0, 1);
    }

    /**
     * Writes bytes to the current ar archive entry.
     *
     * This method simply calls write( byte[], int, int ).
     *
     * @param wBuf The buffer to write to the archive.
     */
    public void write(byte[] wBuf) throws IOException {
        this.write(wBuf, 0, wBuf.length);
    }

    /**
     * Writes bytes to the current ar archive entry. This method
     * is aware of the current entry and will throw an exception if
     * you attempt to write bytes past the length specified for the
     * current entry.
     *
     * @param wBuf The buffer to write to the archive.
     * @param wOffset The offset in the buffer from which to get bytes.
     * @param numToWrite The number of bytes to write.
     */
    public void write(byte[] wBuf, int wOffset, int numToWrite) throws IOException {
        if (!inEntry) {
            throw new IOException("we are not in an entry currently");
        }

        if ((this.currBytes + numToWrite) > this.currSize) {
            throw new IOException("request to write '" + numToWrite
                                  + "' bytes exceeds size in header of '"
                                  + this.currSize + "' bytes");
        }

        if (numToWrite > 0) {
            this.out.write(wBuf,wOffset,numToWrite);
            this.currBytes += numToWrite;
        }
    }
}

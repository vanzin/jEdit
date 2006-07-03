package de.masters_of_disaster.ant.tasks.ar;

import java.io.File;
import java.util.Date;

/**
 * This class represents an entry in an Ar archive. It consists
 * of the entry's header, as well as the entry's File. Entries
 * can be instantiated in one of three ways, depending on how
 * they are to be used.
 * <p>
 * ArEntries that are created from the header bytes read from
 * an archive are instantiated with the ArEntry( byte[] )
 * constructor. These entries will be used when extracting from
 * or listing the contents of an archive. These entries have their
 * header filled in using the header bytes. They also set the File
 * to null, since they reference an archive entry not a file.
 * <p>
 * ArEntries that are created from Files that are to be written
 * into an archive are instantiated with the ArEntry( File )
 * constructor. These entries have their header filled in using
 * the File's information. They also keep a reference to the File
 * for convenience when writing entries.
 * <p>
 * Finally, ArEntries can be constructed from nothing but a name.
 * This allows the programmer to construct the entry by hand, for
 * instance when only an InputStream is available for writing to
 * the archive, and the header information is constructed from
 * other information. In this case the header fields are set to
 * defaults and the File is set to null.
 *
 * <p>
 * The C structure for an Ar Entry's header is:
 * <pre>
 * struct header {
 * char filename[16];
 * char filedate[12];
 * char uid[6];
 * char gid[6];
 * char mode[8];
 * char size[10];
 * char magic[2];
 * } header;
 * </pre>
 *
 */

public class ArEntry implements ArConstants {
    /** The entry's filename. */
    private StringBuffer filename;

    /** The entry's file date. */
    private long fileDate;

    /** The entry's user id. */
    private int userId;

    /** The entry's group id. */
    private int groupId;

    /** The entry's permission mode. */
    private int mode;

    /** The entry's size. */
    private long size;

    /** The entry's magic tag. */
    private StringBuffer magic;

    /** The entry's file reference */
    private File file;

    /** Default permissions bits for files */
    public static final int DEFAULT_FILE_MODE = 0100644;

    /** Convert millis to seconds */
    public static final int MILLIS_PER_SECOND = 1000;

    /**
     * Construct an empty entry and prepares the header values.
     */
    private ArEntry () {
        this.magic = new StringBuffer(HEADERMAGIC);
        this.filename = new StringBuffer();
        this.userId = 0;
        this.groupId = 0;
        this.file = null;
    }

    /**
     * Construct an entry with only a name. This allows the programmer
     * to construct the entry's header "by hand". File is set to null.
     *
     * @param name the entry name
     */
    public ArEntry(String name) {
        this();
        if (name.endsWith("/")) {
        	throw new IllegalArgumentException("ar archives can only contain files");
        }
        this.filename = new StringBuffer(name);
        this.mode = DEFAULT_FILE_MODE;
        this.userId = 0;
        this.groupId = 0;
        this.size = 0;
        this.fileDate = (new Date()).getTime() / MILLIS_PER_SECOND;
    }

    /**
     * Construct an entry for a file. File is set to file, and the
     * header is constructed from information from the file.
     *
     * @param file The file that the entry represents.
     */
    public ArEntry(File file) {
        this();
        if (file.isDirectory()) {
        	throw new IllegalArgumentException("ar archives can only contain files");
        }
        this.file = file;
        this.filename = new StringBuffer(file.getName());
        this.fileDate = file.lastModified() / MILLIS_PER_SECOND;
        this.mode = DEFAULT_FILE_MODE;
        this.size = file.length();
    }

    /**
     * Construct an entry from an archive's header bytes. File is set
     * to null.
     *
     * @param headerBuf The header bytes from an ar archive entry.
     */
    public ArEntry(byte[] headerBuf) {
        this();
        this.parseArHeader(headerBuf);
    }

    /**
     * Determine if the two entries are equal. Equality is determined
     * by the header names being equal.
     *
     * @param it Entry to be checked for equality.
     * @return True if the entries are equal.
     */
    public boolean equals(ArEntry it) {
        return this.getFilename().equals(it.getFilename());
    }

    /**
     * Determine if the two entries are equal. Equality is determined
     * by the header names being equal.
     *
     * @param it Entry to be checked for equality.
     * @return True if the entries are equal.
     */
    public boolean equals(Object it) {
        if (it == null || getClass() != it.getClass()) {
            return false;
        }
        return equals((ArEntry) it);
    }

    /**
     * Hashcodes are based on entry names.
     *
     * @return the entry hashcode
     */
    public int hashCode() {
        return getFilename().hashCode();
    }

    /**
     * Get this entry's name.
     *
     * @return This entry's name.
     */
    public String getFilename() {
        return this.filename.toString();
    }

    /**
     * Set this entry's name.
     *
     * @param name This entry's new name.
     */
    public void setFilename(String filename) {
        this.filename = new StringBuffer(filename);
    }

    /**
     * Set the mode for this entry
     *
     * @param mode the mode for this entry
     */
    public void setMode(int mode) {
        this.mode = mode;
    }

    /**
     * Get this entry's user id.
     *
     * @return This entry's user id.
     */
    public int getUserId() {
        return this.userId;
    }

    /**
     * Set this entry's user id.
     *
     * @param userId This entry's new user id.
     */
    public void setUserId(int userId) {
        this.userId = userId;
    }

    /**
     * Get this entry's group id.
     *
     * @return This entry's group id.
     */
    public int getGroupId() {
        return this.groupId;
    }

    /**
     * Set this entry's group id.
     *
     * @param groupId This entry's new group id.
     */
    public void setGroupId(int groupId) {
        this.groupId = groupId;
    }

    /**
     * Convenience method to set this entry's group and user ids.
     *
     * @param userId This entry's new user id.
     * @param groupId This entry's new group id.
     */
    public void setIds(int userId, int groupId) {
        this.setUserId(userId);
        this.setGroupId(groupId);
    }

    /**
     * Set this entry's modification time. The parameter passed
     * to this method is in "Java time".
     *
     * @param time This entry's new modification time.
     */
    public void setFileDate(long time) {
        this.fileDate = time / MILLIS_PER_SECOND;
    }

    /**
     * Set this entry's modification time.
     *
     * @param time This entry's new modification time.
     */
    public void setFileDate(Date time) {
        this.fileDate = time.getTime() / MILLIS_PER_SECOND;
    }

    /**
     * Get this entry's modification time.
     *
     * @return time This entry's new modification time.
     */
    public Date getFileDate() {
        return new Date(this.fileDate * MILLIS_PER_SECOND);
    }

    /**
     * Get this entry's file.
     *
     * @return This entry's file.
     */
    public File getFile() {
        return this.file;
    }

    /**
     * Get this entry's mode.
     *
     * @return This entry's mode.
     */
    public int getMode() {
        return this.mode;
    }

    /**
     * Get this entry's file size.
     *
     * @return This entry's file size.
     */
    public long getSize() {
        return this.size;
    }

    /**
     * Set this entry's file size.
     *
     * @param size This entry's new file size.
     */
    public void setSize(long size) {
        this.size = size;
    }

    /**
     * Write an entry's header information to a header buffer.
     *
     * @param outbuf The tar entry header buffer to fill in.
     */
    public void writeEntryHeader(byte[] outbuf) {
        int offset = 0;

        offset = ArUtils.getNameBytes(this.filename, outbuf, offset, NAMELEN);
        offset = ArUtils.getLongBytes(this.fileDate, outbuf, offset, FILEDATELEN);
        offset = ArUtils.getIntegerBytes(this.userId, outbuf, offset, UIDLEN);
        offset = ArUtils.getIntegerBytes(this.groupId, outbuf, offset, GIDLEN);
        offset = ArUtils.getOctalBytes(this.mode, outbuf, offset, MODELEN);
        offset = ArUtils.getLongBytes(this.size, outbuf, offset, SIZELEN);
        offset = ArUtils.getNameBytes(this.magic, outbuf, offset, MAGICLEN);

        while (offset < outbuf.length) {
            outbuf[offset++] = 0;
        }
    }

    /**
     * Parse an entry's header information from a header buffer.
     *
     * @param header The ar entry header buffer to get information from.
     */
    public void parseArHeader(byte[] header) {
        throw new UnsupportedOperationException("parseArHeader(byte[]) not yet implmented");
//        int offset = 0;
//
//        this.filename = TarUtils.parseName(header, offset, NAMELEN);
//        offset += NAMELEN;
//        this.fileDate = TarUtils.parseOctal(header, offset, FILEDATELEN);
//        offset += FILEDATELEN;
//        this.userId = (int) TarUtils.parseOctal(header, offset, UIDLEN);
//        offset += UIDLEN;
//        this.groupId = (int) TarUtils.parseOctal(header, offset, GIDLEN);
//        offset += GIDLEN;
//        this.mode = (int) TarUtils.parseOctal(header, offset, MODELEN);
//        offset += MODELEN;
//        this.size = TarUtils.parseOctal(header, offset, SIZELEN);
//        offset += SIZELEN;
//        this.magic = TarUtils.parseName(header, offset, MAGICLEN);
//        offset += MAGICLEN;
    }
}

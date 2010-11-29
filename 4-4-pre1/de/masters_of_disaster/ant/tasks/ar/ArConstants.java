package de.masters_of_disaster.ant.tasks.ar;

/**
 * This interface contains all the definitions used in the package.
 */

public interface ArConstants {
    /**
     * The length of the name field in a file header.
     */
    int    NAMELEN = 16;

    /**
     * The length of the file date field in a file header.
     */
    int    FILEDATELEN = 12;

    /**
     * The length of the user id field in a file header.
     */
    int    UIDLEN = 6;

    /**
     * The length of the group id field in a file header.
     */
    int    GIDLEN = 6;

    /**
     * The length of the mode field in a file header.
     */
    int    MODELEN = 8;

    /**
     * The length of the size field in a file header.
     */
    int    SIZELEN = 10;

    /**
     * The length of the magic field in a file header.
     */
    int    MAGICLEN = 2;

    /**
     * The magic tag put at the end of a file header.
     */
    String HEADERMAGIC = "`\n";

    /**
     * The headerlength of a file header.
     */
    int    HEADERLENGTH = NAMELEN + FILEDATELEN + UIDLEN + GIDLEN + MODELEN + SIZELEN + MAGICLEN;

    /**
     * The length of the magic field in a file header.
     */
    byte[] PADDING = { '\n' };

    /**
     * The magic tag representing an ar archive.
     */
    byte[] ARMAGIC = { '!', '<', 'a', 'r', 'c', 'h', '>', '\n' };
}

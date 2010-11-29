package de.masters_of_disaster.ant.tasks.ar;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Vector;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.MatchingTask;
import org.apache.tools.ant.types.EnumeratedAttribute;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.util.FileUtils;
import org.apache.tools.ant.util.MergingMapper;
import org.apache.tools.ant.util.SourceFileScanner;
import org.apache.tools.zip.UnixStat;

/**
 * Creates an ar archive.
 *
 * @ant.task category="packaging"
 */
public class Ar extends MatchingTask {
    File destFile;
    File baseDir;

    private ArLongFileMode longFileMode = new ArLongFileMode();

    Vector filesets = new Vector();

    /**
     * Indicates whether the user has been warned about long files already.
     */
    private boolean longWarningGiven = false;

    /**
     * Add a new fileset with the option to specify permissions
     * @return the ar fileset to be used as the nested element.
     */
    public ArFileSet createArFileSet() {
        ArFileSet fileset = new ArFileSet();
        filesets.addElement(fileset);
        return fileset;
    }


    /**
     * Set the name/location of where to create the ar file.
     * @param destFile The output of the tar
     */
    public void setDestFile(File destFile) {
        this.destFile = destFile;
    }

    /**
     * This is the base directory to look in for things to ar.
     * @param baseDir the base directory.
     */
    public void setBasedir(File baseDir) {
        this.baseDir = baseDir;
    }

    /**
     * Set how to handle long files, those with a name&gt;16 chars or containing spaces.
     * Optional, default=warn.
     * <p>
     * Allowable values are
     * <ul>
     * <li>  truncate - names are truncated to the maximum length, spaces are replaced by '_'
     * <li>  fail - names greater than the maximum cause a build exception
     * <li>  warn - names greater than the maximum cause a warning and TRUNCATE is used
     * <li>  bsd - BSD variant is used if any names are greater than the maximum.
     * <li>  gnu - GNU variant is used if any names are greater than the maximum.
     * <li>  omit - files with a name greater than the maximum are omitted from the archive
     * </ul>
     * @param mode the mode to handle long file names.
     */
    public void setLongfile(ArLongFileMode mode) {
        this.longFileMode = mode;
    }

    /**
     * do the business
     * @throws BuildException on error
     */
    public void execute() throws BuildException {
        if (destFile == null) {
            throw new BuildException("destFile attribute must be set!",
                                     getLocation());
        }

        if (destFile.exists() && destFile.isDirectory()) {
            throw new BuildException("destFile is a directory!",
                                     getLocation());
        }

        if (destFile.exists() && !destFile.canWrite()) {
            throw new BuildException("Can not write to the specified destFile!",
                                     getLocation());
        }

        Vector savedFileSets = (Vector) filesets.clone();
        try {
            if (baseDir != null) {
                if (!baseDir.exists()) {
                    throw new BuildException("basedir does not exist!",
                                             getLocation());
                }

                // add the main fileset to the list of filesets to process.
                ArFileSet mainFileSet = new ArFileSet(fileset);
                mainFileSet.setDir(baseDir);
                filesets.addElement(mainFileSet);
            }

            if (filesets.size() == 0) {
                throw new BuildException("You must supply either a basedir "
                                         + "attribute or some nested filesets.",
                                         getLocation());
            }

            // check if ar is out of date with respect to each
            // fileset
            boolean upToDate = true;
            for (Enumeration e = filesets.elements(); e.hasMoreElements();) {
                ArFileSet fs = (ArFileSet) e.nextElement();
                String[] files = fs.getFiles(getProject());

                if (!archiveIsUpToDate(files, fs.getDir(getProject()))) {
                    upToDate = false;
                }

                for (int i = 0; i < files.length; ++i) {
                    if (destFile.equals(new File(fs.getDir(getProject()),
                                                files[i]))) {
                        throw new BuildException("An ar file cannot include "
                                                 + "itself", getLocation());
                    }
                }
            }

            if (upToDate) {
                log("Nothing to do: " + destFile.getAbsolutePath()
                    + " is up to date.", Project.MSG_INFO);
                return;
            }

            log("Building ar: " + destFile.getAbsolutePath(), Project.MSG_INFO);

            ArOutputStream aOut = null;
            try {
                aOut = new ArOutputStream(
                    new BufferedOutputStream(
                        new FileOutputStream(destFile)));
                if (longFileMode.isTruncateMode()
                     || longFileMode.isWarnMode()) {
                    aOut.setLongFileMode(ArOutputStream.LONGFILE_TRUNCATE);
                } else if (longFileMode.isFailMode()
                            || longFileMode.isOmitMode()) {
                    aOut.setLongFileMode(ArOutputStream.LONGFILE_ERROR);
                } else if (longFileMode.isBsdMode()) {
                    aOut.setLongFileMode(ArOutputStream.LONGFILE_BSD);
                } else {
                    // GNU
                    aOut.setLongFileMode(ArOutputStream.LONGFILE_GNU);
                }

                longWarningGiven = false;
                for (Enumeration e = filesets.elements();
                     e.hasMoreElements();) {
                    ArFileSet fs = (ArFileSet) e.nextElement();
                    String[] files = fs.getFiles(getProject());
                    if (files.length > 1 && fs.getFullpath().length() > 0) {
                        throw new BuildException("fullpath attribute may only "
                                                 + "be specified for "
                                                 + "filesets that specify a "
                                                 + "single file.");
                    }
                    for (int i = 0; i < files.length; i++) {
                        File f = new File(fs.getDir(getProject()), files[i]);
                        arFile(f, aOut, fs);
                    }
                }
            } catch (IOException ioe) {
                String msg = "Problem creating AR: " + ioe.getMessage();
                throw new BuildException(msg, ioe, getLocation());
            } finally {
                FileUtils.close(aOut);
            }
        } finally {
            filesets = savedFileSets;
        }
    }

    /**
     * ar a file
     * @param file the file to ar
     * @param aOut the output stream
     * @param arFileSet the fileset that the file came from.
     * @throws IOException on error
     */
    protected void arFile(File file, ArOutputStream aOut, ArFileSet arFileSet)
        throws IOException {
        FileInputStream fIn = null;

        if (file.isDirectory()) {
            return;
        }

        String fileName = file.getName();

        String fullpath = arFileSet.getFullpath();
        if (fullpath.length() > 0) {
            fileName = fullpath.substring(fullpath.lastIndexOf('/'));
        }

        // don't add "" to the archive
        if (fileName.length() <= 0) {
            return;
        }

        try {
            if ((fileName.length() >= ArConstants.NAMELEN)
                  || (-1 != fileName.indexOf(' '))) {
                if (longFileMode.isOmitMode()) {
                    log("Omitting: " + fileName, Project.MSG_INFO);
                    return;
                } else if (longFileMode.isWarnMode()) {
                    if (!longWarningGiven) {
                        log("Resulting ar file contains truncated or space converted filenames",
                            Project.MSG_WARN);
                        longWarningGiven = true;
                    }
                    log("Entry: \"" + fileName + "\" longer than "
                        + ArConstants.NAMELEN + " characters or containing spaces.",
                        Project.MSG_WARN);
                } else if (longFileMode.isFailMode()) {
                    throw new BuildException("Entry: \"" + fileName
                        + "\" longer than " + ArConstants.NAMELEN
                        + "characters or containting spaces.", getLocation());
                }
            }

            ArEntry ae = new ArEntry(fileName);
            ae.setFileDate(file.lastModified());
            ae.setUserId(arFileSet.getUid());
            ae.setGroupId(arFileSet.getGid());
            ae.setMode(arFileSet.getMode());
            ae.setSize(file.length());

            aOut.putNextEntry(ae);

            fIn = new FileInputStream(file);

            byte[] buffer = new byte[8 * 1024];
            int count = 0;
            do {
                aOut.write(buffer, 0, count);
                count = fIn.read(buffer, 0, buffer.length);
            } while (count != -1);

            aOut.closeEntry();
        } finally {
            if (fIn != null) {
                fIn.close();
            }
        }
    }

    /**
     * Is the archive up to date in relationship to a list of files.
     * @param files the files to check
     * @param dir   the base directory for the files.
     * @return true if the archive is up to date.
     */
    protected boolean archiveIsUpToDate(String[] files, File dir) {
        SourceFileScanner sfs = new SourceFileScanner(this);
        MergingMapper mm = new MergingMapper();
        mm.setTo(destFile.getAbsolutePath());
        return sfs.restrict(files, dir, null, mm).length == 0;
    }

    /**
     * This is a FileSet with the option to specify permissions
     * and other attributes.
     */
    public static class ArFileSet extends FileSet {
        private String[] files = null;

        private int fileMode = UnixStat.FILE_FLAG | UnixStat.DEFAULT_FILE_PERM;
        private int    uid;
        private int    gid;
        private String fullpath = "";

        /**
         * Creates a new <code>ArFileSet</code> instance.
         * Using a fileset as a constructor argument.
         *
         * @param fileset a <code>FileSet</code> value
         */
        public ArFileSet(FileSet fileset) {
            super(fileset);
        }

        /**
         * Creates a new <code>ArFileSet</code> instance.
         *
         */
        public ArFileSet() {
            super();
        }

        /**
         *  Get a list of files and directories specified in the fileset.
         * @param p the current project.
         * @return a list of file and directory names, relative to
         *    the baseDir for the project.
         */
        public String[] getFiles(Project p) {
            if (files == null) {
                DirectoryScanner ds = getDirectoryScanner(p);
                files = ds.getIncludedFiles();
            }

            return files;
        }

        /**
         * A 3 digit octal string, specify the user, group and
         * other modes in the standard Unix fashion;
         * optional, default=0644
         * @param octalString a 3 digit octal string.
         */
        public void setMode(String octalString) {
            this.fileMode =
                UnixStat.FILE_FLAG | Integer.parseInt(octalString, 8);
        }

        /**
         * @return the current mode.
         */
        public int getMode() {
            return fileMode;
        }

        /**
         * The UID for the ar entry; optional, default="0"
         * @param uid the id of the user for the ar entry.
         */
        public void setUid(int uid) {
            this.uid = uid;
        }

        /**
         * @return the UID for the ar entry
         */
        public int getUid() {
            return uid;
        }

        /**
         * The GID for the ar entry; optional, default="0"
         * @param gid the group id.
         */
        public void setGid(int gid) {
            this.gid = gid;
        }

        /**
         * @return the group identifier.
         */
        public int getGid() {
            return gid;
        }

        /**
         * If the fullpath attribute is set, the file in the fileset
         * is written with the last part of the path in the archive.
         * If the fullpath ends in '/' the file is omitted from the archive.
         * It is an error to have more than one file specified in such a fileset.
         * @param fullpath the path to use for the file in a fileset.
         */
        public void setFullpath(String fullpath) {
            this.fullpath = fullpath;
        }

        /**
         * @return the path to use for a single file fileset.
         */
        public String getFullpath() {
            return fullpath;
        }
    }

    /**
     * Set of options for long file handling in the task.
     */
    public static class ArLongFileMode extends EnumeratedAttribute {
        /** permissible values for longfile attribute */
        public static final String
            WARN = "warn",
            FAIL = "fail",
            TRUNCATE = "truncate",
            GNU = "gnu",
            BSD = "bsd",
            OMIT = "omit";

        private final String[] validModes = {WARN, FAIL, TRUNCATE, GNU, BSD, OMIT};

        /** Constructor, defaults to "warn" */
        public ArLongFileMode() {
            super();
            setValue(WARN);
        }

        /**
         * @return the possible values for this enumerated type.
         */
        public String[] getValues() {
            return validModes;
        }

        /**
         * @return true if value is "truncate".
         */
        public boolean isTruncateMode() {
            return TRUNCATE.equalsIgnoreCase(getValue());
        }

        /**
         * @return true if value is "warn".
         */
        public boolean isWarnMode() {
            return WARN.equalsIgnoreCase(getValue());
        }

        /**
         * @return true if value is "gnu".
         */
        public boolean isGnuMode() {
            return GNU.equalsIgnoreCase(getValue());
        }

        /**
         * @return true if value is "bsd".
         */
        public boolean isBsdMode() {
            return BSD.equalsIgnoreCase(getValue());
        }

        /**
         * @return true if value is "fail".
         */
        public boolean isFailMode() {
            return FAIL.equalsIgnoreCase(getValue());
        }

        /**
         * @return true if value is "omit".
         */
        public boolean isOmitMode() {
            return OMIT.equalsIgnoreCase(getValue());
        }
    }
}

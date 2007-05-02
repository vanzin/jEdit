package de.masters_of_disaster.ant.tasks.calculatesize;

import java.io.File;
import java.util.Enumeration;
import java.util.Vector;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.taskdefs.MatchingTask;
import org.apache.tools.ant.types.FileSet;

/**
 * Calculates the "Installed-Size" of a deb package for the "control"-file.
 *
 * @ant.task category="packaging"
 */
public class CalculateSize extends MatchingTask {
    String realSizeProperty = null;
    String diskSizeProperty = null;
    Vector fileSets = new Vector();
    File baseDir;

    /**
     * Add a new fileset
     * 
     * @return the fileset to be used as the nested element.
     */
    public FileSet createFileSet() {
        FileSet fileSet = new FileSet();
        fileSets.addElement(fileSet);
        return fileSet;
    }

    /**
     * This is the base directory to look in for things to include.
     * 
     * @param baseDir the base directory.
     */
    public void setBaseDir(File baseDir) {
        this.baseDir = baseDir;
        fileset.setDir(baseDir);
    }

    /**
     * This is the property to set to the real size.
     * 
     * @param realSizeProperty The property to set to the real size
     */
    public void setRealSizeProperty(String realSizeProperty) {
        this.realSizeProperty = realSizeProperty;
    }

    /**
     * This is the property to set to the disk size.
     * 
     * @param diskSizeProperty The property to set to the disk size
     */
    public void setDiskSizeProperty(String diskSizeProperty) {
        this.diskSizeProperty = diskSizeProperty;
    }

    /**
     * do the business
     * 
     * @throws BuildException on error
     */
    public void execute() throws BuildException {
        if ((null == realSizeProperty) && (null == diskSizeProperty)) {
            throw new BuildException("realSizeProperty or diskSizeProperty must be set for <CalculateSize>");
        }

        if (null != baseDir) {
            // add the main fileset to the list of filesets to process.
            fileSets.addElement(fileset);
        }

        long realSize = 0;
        long diskSize = 0;
        for (Enumeration e=fileSets.elements() ; e.hasMoreElements() ; ) {
            FileSet fileSet = (FileSet)e.nextElement();
            String[] files = fileSet.getDirectoryScanner(getProject()).getIncludedFiles();
            File fileSetDir = fileSet.getDir(getProject());
            for (int i=0, c=files.length ; i<c ; i++) {
                long fileLength = new File(fileSetDir,files[i]).length();
                realSize += fileLength / 1024;
                diskSize += (fileLength / 4096 + 1) * 4;
            }
        }
        if (null != realSizeProperty) {
            getProject().setNewProperty(realSizeProperty,Long.toString(realSize));
        }
        if (null != diskSizeProperty) {
            getProject().setNewProperty(diskSizeProperty,Long.toString(diskSize));
        }
    }
}

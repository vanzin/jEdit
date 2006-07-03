package de.masters_of_disaster.ant.tasks.deb;

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
public class CalculateInstalledSizeForDeb extends MatchingTask {
    String property = null;
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
     * This is the base directory to look in for things to include.
     * 
     * @param baseDir the base directory.
     */
    public void setProperty(String property) {
        this.property = property;
    }

    /**
     * do the business
     * 
     * @throws BuildException on error
     */
    public void execute() throws BuildException {
        if (null == property) {
            throw new BuildException("property must be set for <CalculateInstalledSizeForDeb>");
        }

        if (null != baseDir) {
            // add the main fileset to the list of filesets to process.
            fileSets.addElement(fileset);
        }

        long totalSize = 0;
        for (Enumeration e=fileSets.elements() ; e.hasMoreElements() ; ) {
            FileSet fileSet = (FileSet)e.nextElement();
            String[] files = fileSet.getDirectoryScanner(getProject()).getIncludedFiles();
            File fileSetDir = fileSet.getDir(getProject());
            for (int i=0, c=files.length ; i<c ; i++) {
                totalSize += new File(fileSetDir,files[i]).length() / 1024;
            }
        }
        getProject().setNewProperty(property,Long.toString(totalSize));
    }
}

package de.masters_of_disaster.ant.tasks.deb;

import de.masters_of_disaster.ant.tasks.ar.Ar;
import de.masters_of_disaster.ant.tasks.ar.Ar.ArFileSet;
import java.io.File;
import java.util.Enumeration;
import java.util.Vector;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.taskdefs.Checksum;
import org.apache.tools.ant.taskdefs.Echo;
import org.apache.tools.ant.taskdefs.Echo.EchoLevel;
import org.apache.tools.ant.taskdefs.Mkdir;
import org.apache.tools.ant.taskdefs.MatchingTask;
import org.apache.tools.ant.taskdefs.Tar;
import org.apache.tools.ant.taskdefs.Tar.TarCompressionMethod;
import org.apache.tools.ant.taskdefs.Tar.TarFileSet;
import org.apache.tools.ant.util.FileUtils;
import org.apache.tools.ant.util.MergingMapper;
import org.apache.tools.ant.util.SourceFileScanner;

/**
 * Creates a deb package.
 *
 * @ant.task category="packaging"
 */
public class Deb extends MatchingTask {
    Vector controlFileSets = new Vector();
    Vector dataFileSets = new Vector();
    File baseDir;
    File destFile;
    File tempDir;
    boolean deleteTempFiles = true;
    boolean includeMd5sums = false;
    Tar controlTarGz = new Tar();
    Tar dataTarGz = new Tar();
    Ar debPackage = new Ar();

    {
        fileset = dataTarGz.createTarFileSet();
    }

    /**
     * Add a new fileset for the control files with the option to specify permissions
     * 
     * @return the tar fileset to be used as the nested element.
     */
    public TarFileSet createControlFileSet() {
        TarFileSet fileSet = controlTarGz.createTarFileSet();
        controlFileSets.addElement(fileSet);
        return fileSet;
    }

    /**
     * Add a new fileset for the data files with the option to specify permissions
     * 
     * @return the tar fileset to be used as the nested element.
     */
    public TarFileSet createDataFileSet() {
        TarFileSet fileSet = dataTarGz.createTarFileSet();
        dataFileSets.addElement(fileSet);
        return fileSet;
    }

    /**
     * Set the name/location of where to create the deb file.
     * 
     * @param destFile The output of the deb
     */
    public void setDestFile(File destFile) {
        this.destFile = destFile;
        debPackage.setDestFile(destFile);
    }

    /**
     * This is the base directory to look in for things to include in the data files.
     * 
     * @param baseDir the base directory.
     */
    public void setBaseDir(File baseDir) {
        this.baseDir = baseDir;
        fileset.setDir(baseDir);
    }

    /**
     * This is the temp directory where to create the temporary files.
     * If not set, the current projects baseDir is used.
     * 
     * @param tempDir the temp directory.
     */
    public void setTempDir(File tempDir) {
        this.tempDir = tempDir;
    }

    /**
     * This specifies if the temporary files should get deleted.
     * 
     * @param deleteTempFiles whether to delete the temporary files or not.
     */
    public void setDeleteTempFiles(boolean deleteTempFiles) {
        this.deleteTempFiles = deleteTempFiles;
    }

    /**
     * This specifies if the MD5 sums of the files in the data section should be
     * included in the file "md5sums" in the control section.
     * 
     * @param includeMd5sums whether to include MD5 sums in the control section or not.
     */
    public void setIncludeMd5sums(boolean includeMd5sums) {
        this.includeMd5sums = includeMd5sums;
    }

    /**
     * do the business
     * 
     * @throws BuildException on error
     */
    public void execute() throws BuildException {
        prepareTask(controlTarGz);
        prepareTask(dataTarGz);
        prepareTask(debPackage);
        TarFileSet tarFileSet = controlTarGz.createTarFileSet();
        tarFileSet.setFile(new File(System.getProperty("user.dir")));
        tarFileSet.setUserName("root");
        tarFileSet.setGroup("root");
        tarFileSet.setFullpath("./");
        tarFileSet = dataTarGz.createTarFileSet();
        tarFileSet.setFile(new File(System.getProperty("user.dir")));
        tarFileSet.setUserName("root");
        tarFileSet.setGroup("root");
        tarFileSet.setFullpath("./");

        if (null == tempDir) {
            tempDir = getProject().getBaseDir();
        }

        if (null != baseDir) {
            // add the main fileset to the list of filesets to process.
            dataFileSets.addElement(fileset);
        } else {
            fileset.setDir(new File(System.getProperty("user.dir")));
            fileset.setExcludes("**");
        }

        boolean controlFound = false;
        for (Enumeration e=controlFileSets.elements() ; e.hasMoreElements() ; ) {
            TarFileSet fileSet = (TarFileSet)e.nextElement();
            String[] files = fileSet.getFiles(getProject());
            int i = 0;
            int c;

            for (c=files.length ; i<c && !controlFound ; i++) {
                if (files[i].endsWith("control")
                      && (new File(fileSet.getDir(getProject()),files[i])).isFile()) {
                    controlFound = true;
                }
            }
        }
        if (!controlFound) {
            throw new BuildException("The control fileset must contain a file \"control\"", getLocation());
        }

        // check if deb is out of date with respect to each fileset
        boolean upToDate = true;
        for (Enumeration e=controlFileSets.elements() ; e.hasMoreElements() ; ) {
            TarFileSet fileSet = (TarFileSet)e.nextElement();
            String[] files = fileSet.getFiles(getProject());

            if (!packageIsUpToDate(files,fileSet.getDir(getProject()))) {
                upToDate = false;
            }
        }

        for (Enumeration e=dataFileSets.elements() ; e.hasMoreElements() ; ) {
            TarFileSet fileSet = (TarFileSet)e.nextElement();
            String[] files = fileSet.getFiles(getProject());

            if (!packageIsUpToDate(files,fileSet.getDir(getProject()))) {
                upToDate = false;
            }
        }

        if (upToDate) {
            log("Nothing to do: " + destFile.getAbsolutePath()
                + " is up to date.", Project.MSG_INFO);
            return;
        }

        log("Building deb: " + destFile.getAbsolutePath(), Project.MSG_INFO);

        Mkdir mkdir = new Mkdir();
        prepareTask(mkdir);
        mkdir.setDir(tempDir);
        mkdir.perform();

        EchoLevel echoLevel = new EchoLevel();
        echoLevel.setValue("error");
        File debianBinaryFile = new File(tempDir,"debian-binary");
        Echo echo = new Echo();
        prepareTask(echo);
        echo.setFile(debianBinaryFile);
        echo.setLevel(echoLevel);
        echo.setMessage("2.0\n");
        echo.perform();

        for (Enumeration e=controlFileSets.elements() ; e.hasMoreElements() ; ) {
            TarFileSet fileSet = (TarFileSet)e.nextElement();
            String prefix = fileSet.getPrefix();
            String fullpath = fileSet.getFullpath();
            if ("".equals(fullpath) && !prefix.startsWith("./")) {
                if (prefix.startsWith("/")) {
                    fileSet.setPrefix("." + prefix);
                } else {
                    fileSet.setPrefix("./" + prefix);
                }
            }
            if ((fullpath.length() > 0) && !fullpath.startsWith("./")) {
                fileSet.setPrefix("");
                if (fullpath.startsWith("/")) {
                    fileSet.setFullpath("." + fullpath);
                } else {
                    fileSet.setFullpath("./" + fullpath);
                }
            }
            if ((0 == fileSet.getUid()) && ("" == fileSet.getUserName())) {
                fileSet.setUserName("root");
            }
            if ((0 == fileSet.getGid()) && ("" == fileSet.getGroup())) {
                fileSet.setGroup("root");
            }
        }

        for (Enumeration e=dataFileSets.elements() ; e.hasMoreElements() ; ) {
            TarFileSet fileSet = (TarFileSet)e.nextElement();
            String prefix = fileSet.getPrefix();
            String fullpath = fileSet.getFullpath();
            if ("".equals(fullpath) && !prefix.startsWith("./")) {
                if (prefix.startsWith("/")) {
                    fileSet.setPrefix("." + prefix);
                } else {
                    fileSet.setPrefix("./" + prefix);
                }
            }
            if ((fullpath.length() > 0) && !fullpath.startsWith("./")) {
                fileSet.setPrefix("");
                if (fullpath.startsWith("/")) {
                    fileSet.setFullpath("." + fullpath);
                } else {
                    fileSet.setFullpath("./" + fullpath);
                }
            }
            if ((0 == fileSet.getUid()) && ("" == fileSet.getUserName())) {
                fileSet.setUserName("root");
            }
            if ((0 == fileSet.getGid()) && ("" == fileSet.getGroup())) {
                fileSet.setGroup("root");
            }
        }

        File md5sumsFile = new File(tempDir,"md5sums");
        if (includeMd5sums) {
            Checksum md5 = new Checksum();
            prepareTask(md5);
            int md5Count = 0;
            StringBuffer md5sums = new StringBuffer();
            for (Enumeration e=dataFileSets.elements() ; e.hasMoreElements() ; ) {
                TarFileSet fileSet = (TarFileSet)e.nextElement();
                String[] files = fileSet.getDirectoryScanner(getProject()).getIncludedFiles();
                File fileSetDir = fileSet.getDir(getProject());
                for (int i=0, c=files.length ; i<c ; i++) {
                    md5.setFile(new File(fileSetDir,files[i]));
                    md5.setProperty("md5_"+md5Count);
                    md5.perform();
                    md5sums.append(getProject().getProperty("md5_"+md5Count)).append("  ");
                    String fullpath = fileSet.getFullpath();
                    if (fullpath.length() > 0) {
                        md5sums.append(fullpath.substring(2));
                    } else {
                        md5sums.append(fileSet.getPrefix().substring(2)).append(files[i].replace('\\','/'));
                    }
                    md5sums.append("\n");
                    md5Count++;
                }
            }
            echo = new Echo();
            prepareTask(echo);
            echo.setFile(md5sumsFile);
            echo.setLevel(echoLevel);
            echo.setMessage(md5sums.toString());
            echo.perform();
            tarFileSet = controlTarGz.createTarFileSet();
            tarFileSet.setFile(md5sumsFile);
            tarFileSet.setUserName("root");
            tarFileSet.setGroup("root");
            tarFileSet.setPrefix("./");
        }

        TarCompressionMethod tarCompressionMethod = new TarCompressionMethod();
        tarCompressionMethod.setValue("gzip");
        controlTarGz.setCompression(tarCompressionMethod);
        File controlTarGzFile = new File(tempDir,"control.tar.gz");
        controlTarGz.setDestFile(controlTarGzFile);
        controlTarGz.perform();

        dataTarGz.setCompression(tarCompressionMethod);
        File dataTarGzFile = new File(tempDir,"data.tar.gz");
        dataTarGz.setDestFile(dataTarGzFile);
        dataTarGz.perform();

        FileUtils.delete(destFile);
        ArFileSet fileSet = debPackage.createArFileSet();
        fileSet.setFile(debianBinaryFile);
        fileSet = debPackage.createArFileSet();
        fileSet.setFile(controlTarGzFile);
        fileSet = debPackage.createArFileSet();
        fileSet.setFile(dataTarGzFile);
        debPackage.perform();

        if (deleteTempFiles) {
            FileUtils.delete(debianBinaryFile);
            FileUtils.delete(controlTarGzFile);
            FileUtils.delete(dataTarGzFile);
            FileUtils.delete(md5sumsFile);
        }
    }

    /**
     * Checks whether the package is up to date in relationship to a list of files.
     * 
     * @param files the files to check
     * @param dir   the base directory for the files.
     * @return true if the archive is up to date.
     */
    protected boolean packageIsUpToDate(String[] files, File dir) {
        SourceFileScanner sfs = new SourceFileScanner(this);
        MergingMapper mm = new MergingMapper();
        mm.setTo(destFile.getAbsolutePath());
        return sfs.restrict(files, dir, null, mm).length == 0;
    }

    /**
     * Prepares a task for execution.
     * 
     * @param task the task to prepare
     */
    protected void prepareTask(Task task) {
        task.setProject(getProject());
        task.setOwningTarget(getOwningTarget());
        task.setTaskName(getTaskName());
        task.setTaskType(getTaskType());
    }
}

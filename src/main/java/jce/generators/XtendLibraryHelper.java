package jce.generators;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.eclipse.core.resources.ICommand;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.pde.core.IEditableModel;
import org.eclipse.pde.core.build.IBuildEntry;
import org.eclipse.pde.core.build.IBuildModel;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.PluginRegistry;

import jce.properties.EcorificationProperties;
import jce.util.ResourceRefresher;
import jce.util.logging.MonitorFactory;
import jce.util.logging.ProgressMonitorAdapter;

/**
 * Helper class that edits an project do add the Xtend dependencies.
 * @author Timur Saglam
 */
public final class XtendLibraryHelper {
    private static final Logger logger = LogManager.getLogger(XtendLibraryHelper.class.getName());
    private static final char SLASH = File.separatorChar;
    private static final String XTEND = "xtend-gen"; // Xtend folder name

    private XtendLibraryHelper() {
        throw new AssertionError("Suppress default constructor for noninstantiability");
    }

    /**
     * Adds the Xtend dependencies to a project and creates the xtend-gen source folder.
     * @param project is the {@link IProject} instance of the project.
     * @param properties are the Ecorification properties.
     */
    public static void addXtendLibs(IProject project, EcorificationProperties properties) {
        logger.info("Adding Xtend dependencies...");
        IProgressMonitor monitor = MonitorFactory.createProgressMonitor(logger, properties);
        ResourceRefresher.refresh(project);
        createXtendFolder(project, monitor);
        addClasspathEntry(project, monitor);
        addBuildProperty(project);
        addManifestEntries(project);
        updateProjectDescription(project);
    }

    /**
     * Adds the Xtend folder (xtend-gen) to the build.properties file.
     */
    private static void addBuildProperty(IProject project) {
        try {
            IPluginModelBase base = PluginRegistry.findModel(project);
            if (base != null) {
                IBuildModel buildModel = PluginRegistry.createBuildModel(base);
                IBuildEntry entry = buildModel.getBuild().getEntry("source..");
                String token = XTEND + SLASH;
                if (entry.contains(token)) {
                    logger.warn("build.properties already contains " + token);
                } else {
                    entry.addToken(token);
                }
                if (buildModel instanceof IEditableModel) { // if saveable
                    ((IEditableModel) buildModel).save(); // save changes
                }
            } else {
                logger.error("Generated project is no plug-in project or contains a malformed manifest.");
            }
        } catch (CoreException exception) {
            logger.error(exception);
        }
    }

    /**
     * Retrieves the class path file from the {@link IJavaProject}, adds an {@link IClasspathEntry} for the xtend-gen
     * source folder and sets the changed content.
     */
    private static void addClasspathEntry(IProject project, IProgressMonitor monitor) {
        IJavaProject javaProject = JavaCore.create(project);
        try {
            IClasspathEntry[] entries = javaProject.getRawClasspath();
            String xtendDirectory = SLASH + javaProject.getElementName() + SLASH + XTEND;
            if (Arrays.asList(entries).contains(xtendDirectory)) {
                logger.warn(".classpath already contains " + xtendDirectory);
            } else {
                IClasspathEntry[] newEntries = new IClasspathEntry[entries.length + 1];
                System.arraycopy(entries, 0, newEntries, 0, entries.length);
                newEntries[entries.length] = JavaCore.newSourceEntry(new org.eclipse.core.runtime.Path(xtendDirectory));
                javaProject.setRawClasspath(newEntries, monitor);
            }
        } catch (JavaModelException exception) {
            logger.error(exception);
        }
    }

    /**
     * Adds Xtend manifest entries to the manifest file.
     * @param project is the {@link IJavaProject}.
     */
    private static void addManifestEntries(IProject project) {
        IPath workspace = ResourcesPlugin.getWorkspace().getRoot().getLocation(); // workspace path
        String folder = workspace.toString() + project.getFolder("META-INF").getFullPath(); // manifest folder
        File file = new File(folder + SLASH + "MANIFEST.MF"); // manifest file
        if (file.exists()) {
            List<String> manifest = readFile(file.toPath());
            List<String> newManifest = editManifest(manifest);
            writeFile(file.toPath(), newManifest);
        } else {
            logger.error("Could not find MANIFEST.MF file in " + folder);
        }
    }

    /**
     * Creates the binary file folder for Xtend. This is the xtend-bin folder.
     */
    private static void createXtendFolder(IProject project, IProgressMonitor monitor) {
        IFolder folder = project.getFolder(XTEND);
        if (!folder.exists()) {
            try {
                folder.create(false, true, monitor);
            } catch (CoreException exception) {
                logger.fatal(exception);
            }
        }
    }

    /**
     * Edits a manifest file in form of a list of manifest entries. Returns a new manifest file list.
     * @param manifest is the original manifest file list.
     * @return the edited manifest file list.
     */
    private static List<String> editManifest(List<String> manifest) {
        List<String> newManifest = new LinkedList<String>();
        for (String line : manifest) {
            newManifest.add(line);
            if (line.contains("Require-Bundle:")) {
                newManifest.add(" com.google.guava,");
                newManifest.add(" org.eclipse.xtext.xbase.lib,");
                newManifest.add(" org.eclipse.xtend.lib,");
                newManifest.add(" org.eclipse.xtend.lib.macro,");
                newManifest.add(" edu.kit.ipd.sdq.activextendannotations,");
            }
        }
        return newManifest;
    }

    /**
     * Reads a file from a path and return its content.
     */
    private static List<String> readFile(Path path) {
        List<String> content = new LinkedList<String>();
        try (BufferedReader reader = Files.newBufferedReader(path)) {
            String line = null;
            while ((line = reader.readLine()) != null) {
                content.add(line);
            }
        } catch (IOException exception) {
            logger.error(exception);
        }
        return content;
    }

    /**
     * Adds the xtext nature and builder command to the .project file of the project.
     */
    private static void updateProjectDescription(IProject project) {
        String builderName = "org.eclipse.xtext.ui.shared.xtextBuilder";
        String xtextNature = "org.eclipse.xtext.ui.shared.xtextNature";
        IProjectDescription description = null;
        try {
            description = project.getDescription();
        } catch (CoreException exception) {
            exception.printStackTrace();
        }
        // add xtext builder:
        ICommand[] commands = description.getBuildSpec();
        ICommand command = description.newCommand();
        command.setBuilderName(builderName);
        if (Arrays.asList(commands).contains(command)) {
            logger.warn(".project already contains " + builderName);
        } else {
            ICommand[] newCommands = new ICommand[commands.length + 1];
            System.arraycopy(commands, 0, newCommands, 0, commands.length);
            newCommands[commands.length] = command;
            description.setBuildSpec(newCommands);
        }
        // Add xtext nature:
        String[] natures = description.getNatureIds();
        if (Arrays.asList(natures).contains(xtextNature)) {
            logger.warn(".project already contains " + xtextNature);
        } else {
            String[] newNatures = new String[natures.length + 1];
            System.arraycopy(natures, 0, newNatures, 0, natures.length);
            newNatures[natures.length] = xtextNature;
            description.setNatureIds(newNatures);
        }
        try {
            project.setDescription(description, new ProgressMonitorAdapter(logger));
        } catch (CoreException exception) {
            logger.fatal(exception);
        }
    }

    /**
     * Writes in file at a specific path from a list of lines.
     */
    private static void writeFile(Path path, List<String> content) {
        try (BufferedWriter writer = Files.newBufferedWriter(path)) {
            for (String line : content) {
                writer.write(line + System.getProperty("line.separator"));
            }
        } catch (IOException exception) {
            logger.error(exception);
        }
    }
}
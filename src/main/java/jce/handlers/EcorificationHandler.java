package jce.handlers;

import org.eclipse.core.resources.IProject;

import eme.handlers.MainHandler;
import jce.JavaCodeEcorification;

/**
 * Handler for starting the Java code ecorification.
 * @author Timur Saglam
 */
public class EcorificationHandler extends MainHandler {

    /**
     * @see eme.handlers.MainHandler#startExtraction(org.eclipse.core.resources.IProject)
     */
    @Override
    protected void startExtraction(IProject project) {
        new JavaCodeEcorification().start(project);
    }
}
/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package controllers.web;

import controllers.DefaultController;
import platform.Environment;
import platform.VersionManager;

/**
 * @author user
 */

public class footer extends DefaultController
{
    public static void index(String bucket)
    {
        setDefaultContentPaths();
        //application type specific UI differences
        String applicationType = Environment.getInstance().getApplicationType();
        renderArgs.put("applicationType", applicationType);
        
        //set release number
        VersionManager versionManager = VersionManager.getInstance();
        renderArgs.put("platformVersion", versionManager.getPlatformVersion());
        renderTemplate(renderArgs.get("HtmlPath") + "/common/templates/eula.html");
    }
}

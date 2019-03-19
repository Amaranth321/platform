/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package controllers.web;

import com.kaisquare.playframework.RenderJavaScript;
import com.kaisquare.playframework.RenderWebFont;
import controllers.DefaultController;
import play.Play;
import play.vfs.VirtualFile;

/**
 * Controller to render static content with special headers like CORS.
 * This is used to make JS and Web Fonts work even through CDN.
 *
 * @author kdp
 */
public class cdn extends DefaultController
{

    public static void javascripts(String filename)
    {
        VirtualFile file = Play.getVirtualFile(request.path);
        if (file == null || file.exists() == false)
        {
            notFound();
        }

        setDefaultContentPaths();
        throw new RenderJavaScript(file.getRealFile());
    }

    public static void fonts(String filename)
    {
        VirtualFile file = Play.getVirtualFile(request.path);
        if (file == null || file.exists() == false)
        {
            notFound();
        }

        setDefaultContentPaths();
        throw new RenderWebFont(file.getRealFile());
    }

}

package controllers.web;

import com.kaisquare.util.HTTPUtil;
import controllers.DefaultController;

//kdp import lib.dao.DaoFactory;
//kdp import lib.dao.UserDao;

/**
 * @author Tan Yee Fan
 */
//@With(WebInterceptor.class)
public class error extends DefaultController
{
    public static void index(Integer status, String bucket)
    {
        String error = "Unknown Error";
        if (status != null)
        {
            String description = HTTPUtil.getStatusDescription(status);
            if (description != null)
            {
                error = description;
            }
        }

        setDefaultContentPaths();
        renderArgs.put("error", error);
        renderArgs.put("bucket", bucket);
        renderTemplate(renderArgs.get("HtmlPath") + "/error/index.html");
    }

}

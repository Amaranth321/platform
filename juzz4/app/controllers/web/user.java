package controllers.web;

import controllers.interceptors.WebInterceptor;
import models.MongoUser;
import models.transients.UserInfo;
import play.mvc.Controller;
import play.mvc.With;

@With(WebInterceptor.class)
public class user extends Controller
{

    public static void list()
    {
        renderTemplate(renderArgs.get("HtmlPath") + "/user/list.html");
    }

    public static void account()
    {
        try
        {
            String userId = renderArgs.get("userId").toString();
            MongoUser user = MongoUser.getById(userId);
            renderArgs.put("login", user.getLogin());
            renderArgs.put("name", user.getName());
            renderArgs.put("email", user.getEmail());
            renderArgs.put("userId", user.getUserId());
            renderArgs.put("phone", user.getPhone());
            renderArgs.put("language", user.getLanguage());

            renderTemplate(renderArgs.get("HtmlPath") + "/account/account.html");
        }
        catch (Exception e)
        {
            notFound();
        }
    }

    public static void add()
    {
        renderTemplate(renderArgs.get("HtmlPath") + "/user/add.html");
    }

    public static void edit(Long id)
    {
        String userId = id.toString();
        MongoUser user = MongoUser.getById(userId);
        UserInfo userDetails = user.getAsUserInfo();
        renderTemplate(renderArgs.get("HtmlPath") + "/user/edit.html", userDetails);
    }

    public static void assignrole(Long id)
    {
        renderArgs.put("userId", id);
        renderTemplate(renderArgs.get("HtmlPath") + "/user/assign_role.html");
    }

    public static void assignvehicle(Long id)
    {
        renderArgs.put("userId", id);
        renderTemplate(renderArgs.get("HtmlPath") + "/user/assign_vehicle.html");
    }

    public static void assigndevice(Long id)
    {
        renderArgs.put("userId", id);
        renderTemplate(renderArgs.get("HtmlPath") + "/user/assign_device.html");
    }
}

package controllers.web;

import controllers.interceptors.WebInterceptor;
import models.backwardcompatibility.InventoryItem;
import models.MongoInventoryItem;
import play.mvc.Controller;
import play.mvc.With;

@With(WebInterceptor.class)
public class inventory extends Controller
{

    public static void list()
    {
        renderTemplate(renderArgs.get("HtmlPath") + "/inventory/list.html");
    }

    public static void inventoryItem()
    {
        renderTemplate(renderArgs.get("HtmlPath") + "/inventory/existing_inventory.html");
    }

    public static void edit(String id)
    {
        MongoInventoryItem mongoInventoryItem = MongoInventoryItem.getById(id);
        // note: var name can't be changed
        InventoryItem inventoryItem = new InventoryItem(mongoInventoryItem);
        renderTemplate(renderArgs.get("HtmlPath") + "/inventory/edit.html", inventoryItem);
    }

}

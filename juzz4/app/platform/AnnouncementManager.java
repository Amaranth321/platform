package platform;

import lib.util.exceptions.ApiException;
import models.Announcement;
import play.Logger;
import play.modules.morphia.Model;

import java.util.ArrayList;
import java.util.List;

public class AnnouncementManager
{

    private static AnnouncementManager instance = null;

    private AnnouncementManager()
    {
    }

    public static AnnouncementManager getInstance()
    {
        if (instance == null)
        {
            instance = new AnnouncementManager();
        }
        return instance;
    }

    /**
     * <b>This function will</b><br>
     * &nbsp&nbsp&nbsp - give list of all announcements <br>
     *
     * @return session-key on success of authentication otherwise empty String
     */
    public List<Announcement> getAnnouncementList()
    {
        return (models.Announcement.findAll());
    }

    /**
     * <b>This function will</b><br>
     * &nbsp&nbsp&nbsp - give list of announcements for a specific domain <br>
     *
     * @param domain        The domain name
     * @param includeGlobal true/false flag whether to include announcements which don't belong to any particular domain
     *
     * @return session-key on success of authentication otherwise empty String
     */
    public List<Announcement> getAnnouncementListForDomain(String domain, boolean includeGlobal)
    {
        Model.MorphiaQuery query = Announcement.q();
        if (includeGlobal)
        {
            List<String> domains = new ArrayList<>();
            domains.add(domain);
            domains.add("");
            query.field("domain").hasAnyOf(domains);
        }
        else
        {
            query.filter("domain", domain);
        }
        return query.asList();
    }

    /**
     * <b>This function will</b><br>
     * &nbsp&nbsp&nbsp - add announcement<br>
     *
     * @param type        Type of announcement such as <em> News/Critical </em>
     * @param description Description of announcement
     * @param domain      Domain for which this announcement is created
     */
    public void addAnnouncement(String type, String description, String domain)
            throws ApiException
    {
        try
        {
            Announcement announcement = new Announcement();
            announcement.type = type;
            announcement.description = description;
            announcement.domain = domain;
            announcement.save();
        }
        catch (Exception e)
        {
            Logger.error(lib.util.Util.getStackTraceString(e));
        }
    }

    /**
     * <b>This function will</b><br>
     * &nbsp&nbsp&nbsp - update announcement<br>
     *
     * @param id          id of announcement
     * @param type        Type of announcement <em> News/Critical </em>
     * @param description Description of announcement
     * @param domain      Domain to which this announcement belongs to
     */
    public void updateAnnouncement(String id, String type, String description, String domain)
            throws ApiException
    {
        try
        {
            Announcement announcement = models.Announcement.findById(id);
            if (announcement == null)
            {
                new ApiException("invalid-announcement-id");
            }
            announcement.type = type;
            announcement.description = description;
            announcement.domain = domain;
            announcement.save();
        }
        catch (Exception e)
        {
            Logger.warn(e, "Exception");
            Logger.error(lib.util.Util.getStackTraceString(e));
        }
    }

    /**
     * <b>This function will</b><br>
     * &nbsp&nbsp&nbsp - update announcement<br>
     *
     * @param id -id of announcement
     *
     * @return true on success otherwise false
     */
    public Boolean removeAnnouncement(String id)
    {
        try
        {
            Announcement announcement = models.Announcement.findById(id);
            if (announcement == null)
            {
                new ApiException("invalid-announcement-id");
            }
            announcement.delete();
            return true;
        }
        catch (Exception e)
        {
            Logger.warn(e, "Exception");
            Logger.error(lib.util.Util.getStackTraceString(e));
            return false;
        }
    }
}

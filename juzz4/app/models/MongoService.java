package models;

import com.google.code.morphia.annotations.Entity;
import play.Logger;
import play.modules.morphia.Model;

import java.util.List;
import java.util.regex.Pattern;

/**
 * @author tbnguyen1407
 */
@Entity(value="Service", noClassnameStored = true)
@Model.NoAutoTimestamp
public class MongoService extends Model
{
    // region fields

    private String name;
    private String version;

    // endregion

    // region getters

    public String getName()
    {
        return this.name;
    }

    public String getVersion()
    {
        return this.version;
    }

    // endregion

    // region setters

    public void setName(String newName)
    {
        this.name = newName;
    }

    public void setVersion(String newVersion)
    {
        this.version = newVersion;
    }

    // endregion

    public MongoService()
    {

    }

    // region public methods

    public static MongoService getByName(String serviceName)
    {
        MongoService svc = MongoService.filter("name", serviceName).get();

        // create if not exists
        if (svc == null)
        {
            svc = new MongoService();
            svc.name = serviceName;
            svc.version = "1";
            svc.save();
            Logger.info("New Service API added (%s) with id (%s)", serviceName, svc.getId());
        }

        return svc;
    }

    public boolean equals(Object o)
    {
        if (o instanceof MongoService)
        {
            MongoService other = (MongoService) o;
            return this.name.equals(other.name);
        }

        return false;
    }

    public String toString()
    {
        return this.name;
    }

    // endregion
}

package models;

import com.google.code.morphia.annotations.Entity;
import lib.util.Util;
import platform.access.FeatureRestriction;
import play.Logger;
import play.modules.morphia.Model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * @author tbnguyen1407
 */
@Entity(value="Feature", noClassnameStored = true)
@Model.NoAutoTimestamp
public class MongoFeature extends Model
{
    // region fields

    private String name;
    private String type;
    private Integer levelOnePosition;
    private Integer levelTwoPosition;
    private List<String> serviceNames;
    private String restriction; // name of {@link platform.access.FeatureRestriction}

    // endregion

    // region getters

    public String getName()
    {
        return this.name;
    }

    public String getType()
    {
        return this.type;
    }

    public Integer getLevelOnePosition()
    {
        return this.levelOnePosition;
    }

    public Integer getLevelTwoPosition()
    {
        return this.levelTwoPosition;
    }

    public FeatureRestriction getRestriction()
    {
        return FeatureRestriction.valueOf(restriction);
    }

    public List<String> getServiceNames()
    {
        return this.serviceNames;
    }

    // endregion

    // region setters

    public void setName(String newName)
    {
        this.name = newName;
    }

    public void setType(String newType)
    {
        this.type = newType;
    }

    public void setLevelOnePosition(Integer newLevelOnePosition)
    {
        this.levelOnePosition = newLevelOnePosition;
    }

    public void setLevelTwoPosition(Integer newLevelTwoPosition)
    {
        this.levelTwoPosition = newLevelTwoPosition;
    }

    public void setRestriction(FeatureRestriction newFeatureRestriction)
    {
        this.restriction = newFeatureRestriction.name();
    }

    public void setServiceNames(List<String> newServiceNames)
    {
        this.serviceNames = newServiceNames;
    }

    // endregion

    public MongoFeature()
    {
        serviceNames = new ArrayList<>();
    }

    public static Comparator<MongoFeature> sortByPosition = new Comparator<MongoFeature>()
    {
        public int compare(MongoFeature f1, MongoFeature f2)
        {
            if (f1.levelOnePosition.equals(f2.levelOnePosition))
            {
                return f1.levelTwoPosition - f2.levelTwoPosition;
            }
            else
            {
                return f1.levelOnePosition - f2.levelOnePosition;
            }
        }
    };

    // region public methods

    public static MongoFeature getByName(String featureName)
    {
        return MongoFeature.q().filter("name", featureName).get();
    }

    public boolean addServiceName(String newServiceName)
    {
        try
        {
            if (!this.serviceNames.contains(newServiceName))
            {
                this.serviceNames.add(newServiceName);
            }
            return true;
        }
        catch (Exception e)
        {
            Logger.error(Util.getStackTraceString(e));
            return false;
        }
    }

    public boolean removeServiceName(String newServiceName)
    {
        try
        {
            this.serviceNames.removeAll(Collections.singleton(newServiceName));
            return true;
        }
        catch (Exception e)
        {
            Logger.error(Util.getStackTraceString(e));
            return false;
        }
    }

    @Override
    public boolean equals(Object f2)
    {
        if (f2 instanceof MongoFeature)
        {
            return this.name.equals(((MongoFeature) f2).name);
        }
        else if (f2 instanceof String)
        {
            return this.name.equals(f2);
        }

        return false;
    }

    @Override
    public String toString()
    {
        return this.name;
    }

    public boolean isAssignableToNodes()
    {
        FeatureRestriction restriction = getRestriction();
        return (restriction != FeatureRestriction.CLOUD_ONLY && restriction != FeatureRestriction.SUPERADMIN_ONLY);
    }
}

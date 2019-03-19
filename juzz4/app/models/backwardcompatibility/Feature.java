package models.backwardcompatibility;

import models.MongoFeature;
import models.MongoService;
import platform.access.FeatureRestriction;
import play.db.jpa.Model;

import javax.persistence.Entity;
import javax.persistence.ManyToMany;
import javax.persistence.Table;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

@Entity
@Table(name = "`features`")
@Deprecated
public class Feature extends Model {
    public String name;
    public String type;
    public int levelOnePosition;
    public int levelTwoPosition;

    @ManyToMany
    public Collection<Service> services;

    /**
     * enum name of {@link platform.access.FeatureRestriction}.
     * Use getter and setter provided
     */
    private String restriction;

    public static Comparator<Feature> sortByPosition = new Comparator<Feature>() {
        public int compare(Feature f1, Feature f2) {
            if (f1.levelOnePosition == f2.levelOnePosition) {
                return f1.levelTwoPosition - f2.levelTwoPosition;
            } else {
                return f1.levelOnePosition - f2.levelOnePosition;
            }
        }
    };

    public Feature() {
        services = new ArrayList<>();
    }

    // for compatibility
    public Feature(MongoFeature mongoFeature)
    {
        this.name = mongoFeature.getName();
        this.type = mongoFeature.getType();
        this.levelOnePosition = mongoFeature.getLevelOnePosition();
        this.levelTwoPosition = mongoFeature.getLevelTwoPosition();
        List<Service> sqlServices = new ArrayList<>();
        for (String serviceName : mongoFeature.getServiceNames())
        {
            MongoService mongoService = MongoService.getByName(serviceName);
            sqlServices.add(new Service(mongoService));
        }
        this.services = sqlServices;
    }

    public FeatureRestriction getRestriction() {
        return FeatureRestriction.valueOf(restriction);
    }

    public void setRestriction(FeatureRestriction restriction) {
        this.restriction = restriction.name();
    }

    @Override
    public boolean equals(Object f2) {
        if (f2 instanceof Feature) {
            return this.name.equals(((Feature) f2).name);
        } else if (f2 instanceof String) {
            return this.name.equals(f2);
        }

        return false;
    }

    @Override
    public String toString() {
        return this.name;
    }

    public boolean isAssignableToNodes() {
        FeatureRestriction restriction = getRestriction();
        return (restriction != FeatureRestriction.CLOUD_ONLY && restriction != FeatureRestriction.SUPERADMIN_ONLY);
    }

}


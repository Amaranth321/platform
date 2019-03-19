package models.backwardcompatibility;

import models.MongoFeature;
import models.MongoRole;
import play.db.jpa.Model;

import javax.persistence.Entity;
import javax.persistence.ManyToMany;
import javax.persistence.Table;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Entity
@Table(name = "`roles`")
@Deprecated
public class Role extends Model
{
    public String name;
    public String description;
    public long bucketId;

    @ManyToMany
    public Collection<Feature> features;

    public Role(Bucket bucket, String name, String description)
    {
        this.name = name;
        this.description = description;
        this.bucketId = bucket.getId();
        features = new ArrayList<>();
    }

    // for compatibility
    public Role(MongoRole mongoRole)
    {
        this.id = Long.parseLong(mongoRole.getRoleId());
        this.name = mongoRole.getName();
        this.description = mongoRole.getDescription();
        this.bucketId = Long.parseLong(mongoRole.getBucketId());

        List<Feature> sqlFeatures = new ArrayList<>();
        for (String featureName : mongoRole.getFeatureNames())
        {
            MongoFeature mongoFeature = MongoFeature.getByName(featureName);
            if (mongoFeature != null)
            {
                sqlFeatures.add(new Feature(mongoFeature));
            }
        }
        this.features = sqlFeatures;
    }

    public String toString()
    {
        return name;
    }

    public List<User> getRoleUsers()
    {
        List<User> roleUsers = new ArrayList<>();
        List<User> bktUsers = User.find("bucketId", bucketId).fetch();
        for (User bktUser : bktUsers)
        {
            if (bktUser.roles.contains(this))
            {
                roleUsers.add(bktUser);
            }
        }

        return roleUsers;
    }
}


package models.backwardcompatibility;

import models.MongoService;
import play.Logger;
import play.db.jpa.Model;

import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table(name = "`services`")
@Deprecated
public class Service extends Model
{
    private String name;
    private String version;

    private Service()
    {
    }

    public Service(MongoService mongoService)
    {
        this.name = mongoService.getName();
        this.version = mongoService.getVersion();
    }

    public static Service getServiceByName(String serviceName)
    {
        Service svc = Service.find("name", serviceName).first();
        if (svc == null || !svc.name.equals(serviceName))
        {
            svc = new Service();
            svc.name = serviceName;
            svc.version = "1";
            svc.save();
            Logger.info("New Service API added (%s)", serviceName);
        }

        return svc;
    }

    public String getName()
    {
        return this.name;
    }

    public boolean equals(Object o)
    {
        if (o instanceof Service)
        {
            Service other = (Service) o;
            return this.name.equals(other.name);
        }

        return false;
    }

    public String toString()
    {
        return this.name;
    }
}

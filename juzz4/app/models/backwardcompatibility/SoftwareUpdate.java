package models.backwardcompatibility;

import com.google.code.morphia.annotations.Entity;
import com.google.code.morphia.annotations.Indexed;
import play.modules.morphia.Model;

@Deprecated
@Entity
public class SoftwareUpdate extends Model
{
    @Indexed
    public String name;
    public String version;
    @Indexed
    public String fileName;
    public String host;
    public int port;
    public long size;
    @Indexed
    public long uploadedDate;
}

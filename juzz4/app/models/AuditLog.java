package models;

import com.google.code.morphia.annotations.Embedded;
import com.google.code.morphia.annotations.Entity;
import com.google.code.morphia.annotations.Index;
import com.google.code.morphia.annotations.Indexes;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import play.modules.morphia.Model;
import play.mvc.Http;

/**
 * @author kdp
 */

@Entity
@Indexes({
    @Index("-timeobject"),
})
public class AuditLog extends Model {

    public String userId;
    public String userName;
    public String bucketId;
    public String bucketName;
    public String serviceName;
    public Long timeobject;
    public String remoteIp;
    public Map<String, Http.Header> headers;
    public Map<String, String> params;
    public String exception;
    public String result;
    

    public AuditLog() {
        userId="";
        userName = "";
        bucketId = "";
        bucketName = "";
        serviceName = "";
        remoteIp = "";
        timeobject = new Date().getTime();
        headers = new HashMap<>();
        params = new HashMap<>();
        exception = "";
        result = "";
    }

}

package models.Analytics;

import com.google.code.morphia.annotations.Entity;
import com.google.code.morphia.annotations.Index;
import com.google.code.morphia.annotations.Indexes;
import models.abstracts.PlatformError;
import models.abstracts.ServerPagedResult;
import platform.db.QueryHelper;
import play.modules.morphia.Model;

/**
 * @author Aye Maung
 */
@Entity
@Indexes({
        @Index("instanceId"),
        @Index("time")
})
public class VcaError extends PlatformError
{
    private final String instanceId;

    public static ServerPagedResult<VcaError> query(String instanceId, int offset, int take)
    {
        Model.MorphiaQuery query = VcaError.q();
        query.filter("instanceId", instanceId);
        query.order("-_created");
        return QueryHelper.preparePagedResult(query, offset, take);
    }

    public VcaError(String instanceId, String source, String error, Long time)
    {
        super(source, error, time);
        this.instanceId = instanceId;
    }
}

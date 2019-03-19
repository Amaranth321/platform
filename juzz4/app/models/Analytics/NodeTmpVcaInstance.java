package models.Analytics;

import com.google.code.morphia.annotations.Entity;
import lib.util.exceptions.ApiException;
import lib.util.exceptions.InvalidEnvironmentException;
import models.MongoDevice;
import platform.Environment;
import platform.analytics.IVcaInstance;
import platform.analytics.VcaInfo;
import platform.analytics.VcaStatus;
import platform.rt.RTFeedManager;
import platform.time.RecurrenceRule;
import play.modules.morphia.Model;

/**
 * These are created on Cloud when a new VCA is added, and the cloud is waiting for node's reply.
 *
 * These temporary instances will be removed when the node's reply has been received.
 *
 * @author Aye Maung
 * @since v4.5
 */
@Entity
public class NodeTmpVcaInstance extends Model implements IVcaInstance
{
    private final VcaInfo vcaInfo;

    public static void addNew(MongoDevice nodeDevice, VcaInfo vcaInfo)
    {
        if (!Environment.getInstance().onCloud())
        {
            throw new InvalidEnvironmentException();
        }

        if (!nodeDevice.isKaiNode())
        {
            throw new IllegalArgumentException();
        }

        new NodeTmpVcaInstance(vcaInfo).save();

        //notify UI
        RTFeedManager.getInstance().vcaInstanceChanged(vcaInfo.getInstanceId(), vcaInfo.getCamera());
    }

    public static MorphiaQuery find(VcaInfo vcaInfo)
    {
        return q().filter("vcaInfo.camera", vcaInfo.getCamera())
                .filter("vcaInfo.appId", vcaInfo.getAppId());
    }

    public static MorphiaQuery findByNode(String nodeCoreId)
    {
        return q().filter("vcaInfo.camera.coreDeviceId", nodeCoreId);
    }

    private NodeTmpVcaInstance(VcaInfo vcaInfo)
    {
        this.vcaInfo = vcaInfo;
    }

    @Override
    public VcaInfo getVcaInfo()
    {
        return vcaInfo;
    }

    @Override
    public boolean migrationRequired()
    {
        return false;
    }

    @Override
    public VcaStatus getStatus()
    {
        return VcaStatus.WAITING;
    }

    @Override
    public void update(String settings, RecurrenceRule recurrenceRule) throws ApiException
    {
        //no actions for temp instance
    }

    @Override
    public void activate() throws ApiException
    {
        //no actions for temp instance
    }

    @Override
    public void deactivate() throws ApiException
    {
        //no actions for temp instance
    }

    @Override
    public void remove() throws ApiException
    {
        //no actions for temp instance
    }

}

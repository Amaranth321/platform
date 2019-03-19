package platform.analytics;

import com.kaisquare.vca.thrift.TVcaInstance;
import lib.util.exceptions.ApiException;
import platform.Environment;
import platform.node.KaiSyncCommandClient;
import platform.pubsub.PlatformEventMonitor;
import platform.pubsub.PlatformEventType;
import platform.time.RecurrenceRule;
import play.Logger;

/**
 * @author Aye Maung
 * @since v4.5
 */
public class LocalVcaInstance implements IVcaInstance
{
    private final VcaInfo vcaInfo;
    private final double releaseNumber;
    private final boolean migrationRequired;
    private final VcaStatus status;

    public static LocalVcaInstance addNew(VcaInfo vcaInfo) throws ApiException
    {
        LocalVcaInstance created = VcaThriftClient.getInstance().addVca(vcaInfo);
        if (created == null)
        {
            throw new ApiException("failed-to-add-vca");
        }
        return created;
    }

    public LocalVcaInstance(TVcaInstance tVcaInstance) throws Exception
    {
        vcaInfo = VcaInfo.fromThrift(tVcaInstance.getVcaInfo());
        releaseNumber = tVcaInstance.getReleaseNumber();
        migrationRequired = tVcaInstance.isUpdateRequired();
        status = VcaStatus.parse(tVcaInstance.getVcaStatus());
    }

    @Override
    public VcaInfo getVcaInfo()
    {
        return vcaInfo;
    }

    @Override
    public boolean migrationRequired()
    {
        return migrationRequired;
    }

    @Override
    public VcaStatus getStatus()
    {
        return status;
    }

    @Override
    public void update(String settings, RecurrenceRule recurrenceRule) throws ApiException
    {
        vcaInfo.setSettings(settings);
        vcaInfo.setRecurrenceRule(recurrenceRule);

        //inform vca server
        if (!VcaThriftClient.getInstance().updateVca(getVcaInfo().getInstanceId(),
                                                     settings,
                                                     recurrenceRule))
        {
            throw new ApiException("failed-to-update-vca");
        }

        //inform cloud
        if (Environment.getInstance().onKaiNode())
        {
            KaiSyncCommandClient.getInstance().nodeVcaUpdated(this);
        }
    }

    @Override
    public void activate() throws ApiException
    {
        if (!VcaThriftClient.getInstance().activateVca(getVcaInfo().getInstanceId()))
        {
            throw new ApiException("failed-to-activate-vca");
        }

        //inform cloud
        if (Environment.getInstance().onKaiNode())
        {
            KaiSyncCommandClient.getInstance().nodeVcaActivated(getVcaInfo().getInstanceId());
        }
    }

    @Override
    public void deactivate() throws ApiException
    {
        if (!VcaThriftClient.getInstance().deactivateVca(getVcaInfo().getInstanceId()))
        {
            throw new ApiException("failed-to-deactivate-vca");
        }

        //inform cloud
        if (Environment.getInstance().onKaiNode())
        {
            KaiSyncCommandClient.getInstance().nodeVcaDeactivated(getVcaInfo().getInstanceId());
        }
    }

    @Override
    public void remove() throws ApiException
    {
        if (!VcaThriftClient.getInstance().removeVca(getVcaInfo().getInstanceId()))
        {
            throw new ApiException("failed-to-remove-vca");
        }

        //inform cloud
        if (Environment.getInstance().onKaiNode())
        {
            KaiSyncCommandClient.getInstance().nodeVcaRemoved(getVcaInfo().getInstanceId());
        }

        //broadcast removal
        PlatformEventMonitor.getInstance().broadcast(PlatformEventType.VCA_REMOVED, getVcaInfo().getInstanceId());
    }

    public double getReleaseNumber()
    {
        return releaseNumber;
    }
}

package platform.devices;

import models.MongoDeviceModel;
import play.Logger;

/**
 * @author Aye Maung
 * @since v4.4
 */
public enum NodeEnv
{
    UBUNTU,
    EMBEDDED;

    public static NodeEnv of(long modelId)
    {
        MongoDeviceModel dbModel = MongoDeviceModel.getByModelId(modelId + "");
        if (dbModel == null || !dbModel.isKaiNode())
        {
            Logger.error("invalid node modelId : " + modelId);
            return null;
        }

        //Node1 only have a single channel
        return dbModel.getChannels() == 1 ? EMBEDDED : UBUNTU;
    }
}

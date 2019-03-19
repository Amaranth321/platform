package platform.devices;

import lib.util.exceptions.ApiException;

/**
 * @author Aye Maung
 * @since v4.4
 */
public enum DeviceGroupType
{
    LABELS,
    DEVICES;

    public static DeviceGroupType parse(String typeString) throws ApiException
    {
        for (DeviceGroupType type : values())
        {
            if (type.name().equalsIgnoreCase(typeString))
            {
                return type;
            }
        }

        throw new ApiException("invalid-selection-type");
    }
}

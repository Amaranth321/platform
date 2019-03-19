package platform.coreengine;

import lib.util.exceptions.ApiException;

/**
 * @author Aye Maung
 * @since v4.4
 */
public enum StreamType
{
    HTTP_JPEG("http/jpeg"),
    HTTP_MJPEG("http/mjpeg"),
    HTTP_H264("http/h264"),
    RTSP_H264("rtsp/h264"),
    RTMP_H264("rtmp/h264");

    public static StreamType parse(String coreStreamType) throws ApiException
    {
        for (StreamType streamType : values())
        {
            if (streamType.getCoreStreamType().equals(coreStreamType))
            {
                return streamType;
            }
        }

        throw new ApiException("invalid-stream-type");
    }

    private final String coreStreamType;

    private StreamType(String coreStreamType)
    {
        this.coreStreamType = coreStreamType;
    }

    public String getCoreStreamType()
    {
        return coreStreamType;
    }

    @Override
    public String toString()
    {
        return coreStreamType;
    }
}

package platform.coreengine;

import core.CoreClient;
import lib.util.Util;
import lib.util.exceptions.ApiException;
import platform.devices.DeviceChannelPair;
import platform.time.UtcPeriod;
import play.Logger;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Aye Maung
 * @since v4.4
 */
public enum StreamingManager
{
    INSTANCE;

    private static final int DEFAULT_URL_TTL_SECONDS = 5 * 60;
    private static final int MAX_URL_TTL_SECONDS = 60 * 60;

    private final Map<String, CoreSession> coreSessionMap = new ConcurrentHashMap<>();

    public static StreamingManager getInstance()
    {
        return INSTANCE;
    }

    public CoreSession startLiveStreamSession(DeviceChannelPair camera,
                                              StreamType streamType,
                                              int ttlSeconds,
                                              String clientIp) throws Exception
    {
        //prepare params
        String streamingSessionKey = UUID.randomUUID().toString();
        ttlSeconds = getVerifiedTtl(ttlSeconds);
        String coreChannelId = CoreUtils.getCameraCoreChannelId(camera);

        List<String> urlList = CoreClient.getInstance().streamControlClient.beginStreamSession(
                streamingSessionKey,
                ttlSeconds,
                streamType.getCoreStreamType(),
                Arrays.asList(clientIp),
                camera.getCoreDeviceId(),
                coreChannelId,
                null,
                null
        );

        if (urlList == null || urlList.isEmpty())
        {
            throw new Exception("Invalid url list returned");
        }

        //save core session
        CoreSession coreSession = new CoreSession(urlList, streamingSessionKey, ttlSeconds, clientIp);
        coreSessionMap.put(streamingSessionKey, coreSession);

        return coreSession;
    }

    public CoreSession startPlaybackStreamSession(DeviceChannelPair camera,
                                                  UtcPeriod period,
                                                  StreamType streamType,
                                                  int ttlSeconds,
                                                  String clientIp) throws Exception
    {
        //prepare params
        String streamingSessionKey = UUID.randomUUID().toString();
        ttlSeconds = getVerifiedTtl(ttlSeconds);
        String coreChannelId = CoreUtils.getCameraCoreChannelId(camera);

        List<String> urlList = CoreClient.getInstance().streamControlClient.beginStreamSession(
                streamingSessionKey,
                ttlSeconds,
                streamType.getCoreStreamType(),
                Arrays.asList(clientIp),
                camera.getCoreDeviceId(),
                coreChannelId,
                period.getFromTime(),
                period.getToTime()
        );

        if (Util.isNullOrEmpty(urlList))
        {
            throw new Exception("core returned NULL/empty list");
        }

        //save core session
        CoreSession coreSession = new CoreSession(urlList, streamingSessionKey, ttlSeconds, clientIp);
        coreSessionMap.put(streamingSessionKey, coreSession);

        return coreSession;
    }

    public CoreSession getCameraSnapshot(DeviceChannelPair camera,
                                         int ttlSeconds,
                                         String clientIp) throws Exception
    {
        CoreSession coreSession = startLiveStreamSession(camera, StreamType.HTTP_JPEG, ttlSeconds, clientIp);
        String url = coreSession.getUrlList().get(0);
        if (!Util.isValidImageUrl(url))
        {
            Logger.error(Util.whichFn() + url);
            throw new ApiException("invalid-image-url");
        }

        return coreSession;
    }

    public boolean keepSessionAlive(String streamSessionKey) throws Exception
    {
        boolean result = true;
        if (!coreSessionMap.containsKey(streamSessionKey))
        {
            throw new ApiException("streaming-session-not-found");
        }

        CoreSession coreSession = coreSessionMap.get(streamSessionKey);
        result = CoreClient.getInstance().streamControlClient.keepStreamSessionAlive(
                streamSessionKey,
                coreSession.getTtlSeconds(),
                Arrays.asList(coreSession.getClientIp())
        );

        return result;
    }

    public void expireSession(String streamSessionKey) throws Exception
    {
        CoreClient.getInstance().streamControlClient.endStreamSession(streamSessionKey);
    }

    private int getVerifiedTtl(int ttlSeconds)
    {
        ttlSeconds = ttlSeconds <= 0 ? DEFAULT_URL_TTL_SECONDS : ttlSeconds;
        ttlSeconds = ttlSeconds > MAX_URL_TTL_SECONDS ? MAX_URL_TTL_SECONDS : ttlSeconds;
        return ttlSeconds;
    }
}

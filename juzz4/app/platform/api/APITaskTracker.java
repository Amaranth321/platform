package platform.api;


import lib.util.Util;
import platform.api.progress.ProgressToken;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * @author Aye Maung
 * @since v4.4
 */
public class APITaskTracker
{
    private static final APITaskTracker instance = new APITaskTracker();

    private final Set<ProgressToken> progressTokens = new LinkedHashSet<>();

    public static APITaskTracker getInstance()
    {
        return instance;
    }

    /**
     * Runs a task as a job, with the progress listener.
     * Progress can be monitored with trackrunningtasks WS API
     *
     * @param task
     */
    public synchronized ProgressToken startTaskWithListener(AsyncAPITask task)
    {
        ProgressToken token = new ProgressToken(task);
        progressTokens.add(token);
        task.start();
        return token;
    }

    /**
     * @param userId  User who submitted the task
     * @param apiList [optional] APIs that initiated the task
     */
    public synchronized Set<ProgressToken> find(long userId, List<String> apiList)
    {
        Set<ProgressToken> targetTokens = new LinkedHashSet<>();
        Set<ProgressToken> completedTokens = new LinkedHashSet<>();

        for (ProgressToken token : progressTokens)
        {
            if (token.isTerminated())
            {
                completedTokens.add(token);
                continue;
            }
            if (token.getUserId() == userId)
            {
                if (!Util.isNullOrEmpty(apiList) && !apiList.contains(token.getApi()))
                {
                    continue;
                }
                targetTokens.add(token);
            }
        }

        progressTokens.removeAll(completedTokens);
        return targetTokens;
    }

    private APITaskTracker()
    {
    }
}

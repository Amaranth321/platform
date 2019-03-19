package platform.api.progress;

/**
 * @author Aye Maung
 * @since v4.4
 */
public interface ProgressListener
{
    void taskChanged(String taskName);

    void percentChanged(int newPercent);

    void terminated(boolean withError);
}

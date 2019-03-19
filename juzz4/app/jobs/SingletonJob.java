package jobs;

/**
 * @author Aye Maung
 */
public interface SingletonJob
{
    void start();

    int getFreqSeconds();

    String getPrintedStatus();
}

package platform.dashboard;

/**
 * @author Aye Maung
 * @since v4.4
 */
public class FaceIndexingSummary
{
    private final int total;
    private final float avgDuration;

    public static FaceIndexingSummary getEmptySummary()
    {
        return new FaceIndexingSummary(0, 0);
    }

    public FaceIndexingSummary(int total, float avgDuration)
    {
        this.total = total;
        this.avgDuration = avgDuration;
    }

    public int getTotal()
    {
        return total;
    }

    public float getAvgDuration()
    {
        return avgDuration;
    }
}

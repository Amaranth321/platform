package platform.dashboard;

/**
 * {@link #ageGroupTotals} are in the ascending order of age groups. Refer to {@link #getTotalByAgeGroup(int)}
 *
 * @author Aye Maung
 * @since v4.4
 */
public class ProfilingSummary
{
    private final int maleTotal;
    private final int femaleTotal;
    private final int happyTotal;
    private final int neutralTotal;
    private final int[] ageGroupTotals;

    public static ProfilingSummary getEmptySummary()
    {
        return new ProfilingSummary(0, 0, 0, 0, new int[]{0, 0, 0, 0});
    }

    public ProfilingSummary(int maleTotal,
                            int femaleTotal,
                            int happyTotal,
                            int neutralTotal,
                            int[] ageGroupTotals)
    {
        if (ageGroupTotals.length != 4)
        {
            throw new IllegalArgumentException();
        }

        this.maleTotal = maleTotal;
        this.femaleTotal = femaleTotal;
        this.happyTotal = happyTotal;
        this.neutralTotal = neutralTotal;
        this.ageGroupTotals = ageGroupTotals;
    }

    public int getMaleTotal()
    {
        return maleTotal;
    }

    public int getFemaleTotal()
    {
        return femaleTotal;
    }

    public int getHappyTotal()
    {
        return happyTotal;
    }

    public int getNeutralTotal()
    {
        return neutralTotal;
    }

    /**
     * @param groupIndex 0,1,2,3 allowed for [below 20, 21-35, 36-55, above 55] respectively
     */
    public int getTotalByAgeGroup(int groupIndex)
    {
        if (groupIndex < 0 || groupIndex >= ageGroupTotals.length)
        {
            throw new IllegalArgumentException();
        }
        return ageGroupTotals[groupIndex];
    }
}

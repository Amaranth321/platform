package platform.analytics;

/**
 * @author Aye Maung
 * @since v4.4
 */
public enum VcaGroup
{
    SECURITY,
    BI;

    @Override
    public String toString()
    {
        return name();
    }
}

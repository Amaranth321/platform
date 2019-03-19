package platform.coreengine;

/**
 * @author Aye Maung
 * @since v4.4
 */
public enum MediaType
{
    VIDEO("video"),
    IMAGE("image");

    private final String nameString;

    private MediaType(String nameString)
    {
        this.nameString = nameString;
    }

    @Override
    public String toString()
    {
        return nameString;
    }
}

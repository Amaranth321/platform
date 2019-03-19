package platform.content.delivery;

/**
 * @author Aye Maung
 * @since v4.3
 */
public class Deliverable<T>
{
    private final T details;

    public Deliverable(T details)
    {
        this.details = details;
    }

    public T getDetails()
    {
        return details;
    }
}

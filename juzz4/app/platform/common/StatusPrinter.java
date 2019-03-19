package platform.common;

/**
 * When implementing this function, keep in mind that data on memory will not be correctly shown
 * under load-balanced environment
 *
 * @author Aye Maung
 * @since v4.4
 */
public interface StatusPrinter
{
    String getPrintedStatus();
}

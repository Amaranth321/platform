package platform.content.delivery;

/**
 * @author Aye Maung
 * @since v4.5
 */
public interface EventNotifier
{
    boolean onScreen();

    boolean toEmail();

    boolean toSMS();

    boolean toMobilePush();
}

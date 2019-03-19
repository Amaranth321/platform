package platform.rt;

import java.util.Map;

/**
 * @author Aye Maung
 * @since v4.4
 */
public interface RTFeed
{
    String json();

    Map toAPIOutput();
}

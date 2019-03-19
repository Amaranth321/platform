package platform.analytics;

import lib.util.exceptions.ApiException;
import platform.time.RecurrenceRule;

/**
 * DO NOT allow switching program type. Each vca program has their own internal implementation (e.g. cache).
 * If program type needs to be changed, create a new instance instead.
 *
 * @author Aye Maung
 * @since v4.5
 */
public interface IVcaInstance
{
    VcaInfo getVcaInfo();

    VcaStatus getStatus();

    boolean migrationRequired();

    void update(String settings, RecurrenceRule recurrenceRule) throws ApiException;

    void activate() throws ApiException;

    void deactivate() throws ApiException;

    void remove() throws ApiException;
}

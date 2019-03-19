package platform;

import models.access.PasswordResetKey;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import play.Logger;

import java.util.Date;

//A simple Session Manager implementation using LinkedHashMap. Nothing fancy here.
public class PasswordResetManager
{

    public static final long RESET_KEY_TTL_HOURS = 3;

    //Singleton instance
    private static PasswordResetManager instance = null;

    private PasswordResetManager()
    {
    }

    //Get session manager instance
    public static PasswordResetManager getInstance()
    {
        if (instance == null)
        {
            instance = new PasswordResetManager();
        }

        return instance;
    }

    /**
     * Create a new password reset key, returns key as string
     *
     * @param userId   The user ID for which to create the key.
     * @param bucketId The bucket ID for which to create the key.
     *
     * @return The key as a string.
     */
    public String createPasswordResetKey(Long userId, Long bucketId)
    {
        PasswordResetKey key = new PasswordResetKey(userId, bucketId);
        key.save();
        return key.resetKey;
    }

    /**
     * Validates a password reset key.
     *
     * @param resetKey The key to validate.
     *
     * @return false if resetKey is invalid or is expired, true otherwise.
     */
    public boolean isKeyValid(String resetKey)
    {
        PasswordResetKey key = PasswordResetKey.find("resetKey", resetKey).first();
        if (key == null)
        {
            Logger.warn("Key not found");
            return false;
        }

        Date now = new Date();
        if (key.expiryTime.before(now))
        {
            return false;
        }

        return true;
    }

    /**
     * Retrieve details associated with a password reset key.
     *
     * @param resetKey The password key.
     *
     * @return If reset key is valid, reset key object of type PasswordResetKey is returned; otherwise null.
     */
    public PasswordResetKey getKeyDetails(String resetKey)
    {
        try
        {
            PasswordResetKey key = PasswordResetKey.find("resetKey", resetKey).first();
            return key;
        }
        catch (Exception e)
        {
            Logger.warn(e.getMessage());
            return null;
        }
    }

    /**
     * Immediately invalidates and removes a password reset key if it is present in the system.
     *
     * @param resetKey
     *
     * @return true if key is found and invalidated (removed), false if key is not found.
     */
    public boolean invalidateKey(String resetKey)
    {
        PasswordResetKey key = PasswordResetKey.find("resetKey", resetKey).first();
        if (key == null)
        {
            Logger.warn("Key not found");
            return false;
        }

        key.delete();
        return true;
    }

    /**
     * Remove expired keys
     */
    public void removeExpiredKeys()
    {
        Date now = DateTime.now(DateTimeZone.UTC).toDate();
        PasswordResetKey.q().filter("expiryTime <", now).delete();
    }
}

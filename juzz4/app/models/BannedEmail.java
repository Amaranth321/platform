package models;

import com.google.code.morphia.annotations.Entity;
import com.google.code.morphia.annotations.Index;
import com.google.code.morphia.annotations.Indexes;
import play.modules.morphia.Model;

/**
 * @author Aye Maung
 */
@Entity
@Indexes({
        @Index("email"),
        @Index("email, failCount")
})
public class BannedEmail extends Model
{
    private static final int FAIL_LIMIT = 10;

    private final String email;
    private int failCount;

    public static void recordResult(String email, boolean successful)
    {
        BannedEmail existing = BannedEmail.q().filter("email", email).first();
        if (existing == null)
        {
            if (successful)
            {
                return;
            }
            existing = new BannedEmail(email);
        }

        if (successful)
        {
            existing.delete();
            return;
        }

        existing.failCount++;
        existing.save();
    }

    public static boolean isBanned(String email)
    {
        BannedEmail banned = BannedEmail.q()
                .filter("email", email)
                .filter("failCount >=", FAIL_LIMIT)
                .first();
        return banned != null;
    }

    private BannedEmail(String email)
    {
        this.email = email;
        this.failCount = 0;
    }
}

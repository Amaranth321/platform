package models.transients;

/**
 *
 * @author kdp
 * 
 * Objects of this class do not persist. This class is used merely to create transport objects.
 */
public class UserInfo {

    public String name;
    public String login;
    public String email;
    public Long userId;
    public String roles;
    public String joinedLabels;
    public String phone;
    public boolean activated;
    public Long timeobject;
    public String remoteIP;
    public String browser;

    public UserInfo() {
    }
}

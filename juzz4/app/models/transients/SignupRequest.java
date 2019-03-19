package models.transients;

import org.joda.time.DateTime;

public class SignupRequest {

	public String registrationNumber;
	public String emailVerificationKey;
    public String companyId;
    public String email;
    public String password;
    public DateTime expiryTime;
}


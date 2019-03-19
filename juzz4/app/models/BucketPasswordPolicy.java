package models;

import java.util.List;

import org.joda.time.DateTime;

import lib.util.exceptions.ApiException;
import play.Logger;
import play.i18n.Messages;
import play.modules.morphia.Model;

import com.google.code.morphia.annotations.Entity;

/**
 * @author Keith
 */

@Entity
public class BucketPasswordPolicy extends Model {
	private final long bucketId;
	private boolean requiredUppercase;
	private boolean requiredLowercase;
	private boolean requiredNumeric;
	private boolean requiredSpecialChar;
	private boolean enabledPasswordExpiration;
	private boolean preventedPasswordReuse;
	private boolean emailWhenPasswordExpired;
	private boolean requiredFirstLoginPasswordCheck;

	private int minimumPasswordLength;
	private int passwordExpirationDays;
	private int numberOfReusePasswordPrevention;
	
	public BucketPasswordPolicy(long bucketId){
		this.bucketId = bucketId; 
	}

	public BucketPasswordPolicy findOrCreate() {
		BucketPasswordPolicy bucketPasswordPolicy = BucketPasswordPolicy.find("bucketId", this.bucketId).first();
		if(bucketPasswordPolicy==null){
			bucketPasswordPolicy = new BucketPasswordPolicy(this.bucketId);
			bucketPasswordPolicy.requiredUppercase = false;
			bucketPasswordPolicy.requiredLowercase = false;
			bucketPasswordPolicy.requiredNumeric = false;
			bucketPasswordPolicy.requiredSpecialChar = false;
			bucketPasswordPolicy.enabledPasswordExpiration = false;
			bucketPasswordPolicy.preventedPasswordReuse = false;
			bucketPasswordPolicy.emailWhenPasswordExpired = false;
			bucketPasswordPolicy.requiredFirstLoginPasswordCheck = false;
			bucketPasswordPolicy.minimumPasswordLength = 8;
			bucketPasswordPolicy.passwordExpirationDays = 15;
			bucketPasswordPolicy.numberOfReusePasswordPrevention = 1;
			bucketPasswordPolicy.save();
		}
		return bucketPasswordPolicy;
	}

	public int getMinimumPasswordLength() {
		return minimumPasswordLength;
	}

	public int getNumberOfReusePasswordPrevention() {
		return numberOfReusePasswordPrevention;
	}

	public int getPasswordExpirationDays() {
		return passwordExpirationDays;
	}

	public boolean isEmailWhenPasswordExpired() {
		return emailWhenPasswordExpired;
	}

	public boolean isEnabledPasswordExpiration() {
		return enabledPasswordExpiration;
	}

	public boolean isPreventedPasswordReuse() {
		return preventedPasswordReuse;
	}

	public boolean isRequiredFirstLoginPasswordCheck() {
		return requiredFirstLoginPasswordCheck;
	}

	public boolean isRequiredLowercase() {
		return requiredLowercase;
	}

	public boolean isRequiredNumeric() {
		return requiredNumeric;
	}

	public boolean isRequiredSpecialChar() {
		return requiredSpecialChar;
	}

	public boolean isRequiredUppercase() {
		return requiredUppercase;
	}

	public void setEmailWhenPasswordExpired(boolean emailWhenPasswordExpired) {
		this.emailWhenPasswordExpired = emailWhenPasswordExpired;
	}

	public void setEnabledPasswordExpiration(boolean enabledPasswordExpiration) {
		this.enabledPasswordExpiration = enabledPasswordExpiration;
	}

	public void setMinimumPasswordLength(int minimumPasswordLength) {
		this.minimumPasswordLength = minimumPasswordLength;
	}

	public void setNumberOfReusePasswordPrevention(
			int numberOfReusePasswordPrevention) {
		this.numberOfReusePasswordPrevention = numberOfReusePasswordPrevention;
	}

	public void setPasswordExpirationDays(int passwordExpirationDays) {
		this.passwordExpirationDays = passwordExpirationDays;
	}

	public void setPreventedPasswordReuse(boolean preventedPasswordReuse) {
		this.preventedPasswordReuse = preventedPasswordReuse;
	}

	public void setRequiredFirstLoginPasswordCheck(
			boolean requiredFirstLoginPasswordCheck) {
		this.requiredFirstLoginPasswordCheck = requiredFirstLoginPasswordCheck;
	}

	public void setRequiredLowercase(boolean requiredLowercase) {
		this.requiredLowercase = requiredLowercase;
	}

	public void setRequiredNumeric(boolean requiredNumeric) {
		this.requiredNumeric = requiredNumeric;
	}

	public void setRequiredSpecialChar(boolean requiredSpecialChar) {
		this.requiredSpecialChar = requiredSpecialChar;
	}
	
	public void setRequiredUppercase(boolean requiredUppercase) {
		this.requiredUppercase = requiredUppercase;
	}
	
	public void validateBucketPasswordPolicy(String plainPassword, String encyptedPassword) throws ApiException {
		
		if(!validatePasswordLength(plainPassword)){
			throw new ApiException(Messages.get("invalid-minimum-password-length",this.minimumPasswordLength));
		}
		
		if(!validateUppercase(plainPassword)){
			throw new ApiException("require-uppercase-letter");
		}
		
		if(!validateLowercase(plainPassword)){
			throw new ApiException("require-lowercase-letter");
		}
		
		if(!validateNumeric(plainPassword)) {
			throw new ApiException("require-numberic");
		}
		
		if(!validateSpecialCharacter(plainPassword)) {
			throw new ApiException("require-non-alphanumeric");
		}

	}
	
	public boolean validateFirstLoginPasswordCheck(long userId){
		if(this.requiredFirstLoginPasswordCheck){
			UserPasswordHistory passHistory = new UserPasswordHistory(userId).q()
				.filter("userId", userId)
				.order("-_created")
				.first();
			if(passHistory != null && passHistory.isUserCreation()) {
				return false;
			}
		}
		return true;
	}
	
	private boolean validateLowercase(String plainPassword){
		boolean hasLowercase = !plainPassword.equals(plainPassword.toUpperCase());
		if(this.requiredLowercase && !hasLowercase){
			Logger.warn("Password Policy validation failed: %s", "Missing lowercase");
			return false;
		}
		return true;
	}
	
	private boolean validateNumeric(String plainPassword){
		boolean hasNumeric = plainPassword.matches(".*\\d.*");
		if(this.requiredNumeric && !hasNumeric) {
			Logger.warn("Password Policy validation failed: %s", "Missing numeric");
			return false;
		}
		return true;
	}
	
	public boolean validatePasswordExpiration(long userId) {
		
		//if no enabled expiration check
		if(!this.enabledPasswordExpiration) {
			return true;
		}
		//check user password expire time
		UserPasswordHistory pass = new UserPasswordHistory(userId).getLatestPasswordHistory();
		if(pass == null) {
			return true;
		}
		
		long milisTime = this.passwordExpirationDays * 24 * 60 * 60 * 1000l;
		if((new DateTime().getMillis() - milisTime) > pass._getCreated()){
			Logger.warn("Password Policy validation failed: %s, user id: %s, created time: %s, expire time: %s", 
					"password expired",
					userId,
					pass._getCreated(),
					(new DateTime().getMillis() - milisTime));
			return false;
		}
		
		return true;
	}
	
	private boolean validatePasswordLength(String plainPassword){
		if(this.minimumPasswordLength > plainPassword.length()){
			Logger.warn("Password Policy validation failed: %s", "Minimum password length");
			return false;
		}
		return true;
	}
	
	public void validatePasswordPolicyByUser(long userId, String plainPassword, String encyptedPassword) throws ApiException {
		
		validateBucketPasswordPolicy(plainPassword, encyptedPassword);
		
		if(!validatePasswordReuse(userId, plainPassword, encyptedPassword)){
			throw new ApiException("password-cannot-be-reuse");
		}
	}
	
	private boolean validatePasswordReuse(long userId, String plainPassword, String encyptedPassword){
		if(this.preventedPasswordReuse){
			List <UserPasswordHistory> passHistories = new UserPasswordHistory(userId).q()
			.filter("userId", userId)
			.order("-_created")
			.limit(this.numberOfReusePasswordPrevention)
			.asList();
			
			if(passHistories != null && passHistories.size() > 0) {
				for(UserPasswordHistory oldPass : passHistories) {
					if(oldPass.getPassword().equals(encyptedPassword)) {
						Logger.warn("Password Policy validation failed: %s, (%s)", "Password has been used before", userId);
						return false;
					}
				}
			}
		}
		return true;
	}
	
	private boolean validateSpecialCharacter(String plainPassword){
		boolean hasSpecial   = !plainPassword.matches("[A-Za-z0-9 ]*");
		if(this.requiredSpecialChar && !hasSpecial) {
			Logger.warn("Password Policy validation failed: %s", "Missing special character");
			return false;
		}
		return true;
	}
	
	private boolean validateUppercase(String plainPassword){
		boolean hasUppercase = !plainPassword.equals(plainPassword.toLowerCase());
		if(this.requiredUppercase && !hasUppercase){
			Logger.warn("Password Policy validation failed: %s", "Missing uppercase");
			return false;
		}
		return true;
	}
}

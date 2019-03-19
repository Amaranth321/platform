package models;

import java.util.List;

import com.google.code.morphia.annotations.Entity;
import com.google.code.morphia.annotations.Index;
import com.google.code.morphia.annotations.Indexes;

import play.modules.morphia.Model;

/**
 * @author Keith
 */

@Entity
@Indexes({
    @Index("userId, -_created")
})
public class UserPasswordHistory  extends Model {
	private final long userId;
	private String password;
	private boolean userCreation;
	
	public UserPasswordHistory(long userId){
		this.userId = userId;
	}
	
	public UserPasswordHistory(long userId, String password, boolean userCreation){
		this.userId = userId;
		this.password = password;
		this.userCreation = userCreation;
	}

	public UserPasswordHistory getLatestPasswordHistory(){
		return new UserPasswordHistory(userId).q()
				.filter("userId", userId)
				.order("-_created").first();
	}

	public String getPassword() {
		return password;
	}

	public long getUserId() {
		return userId;
	}
	
	public boolean isUserCreation() {
		return userCreation;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public void setUserCreation(boolean userCreation) {
		this.userCreation = userCreation;
	}
}

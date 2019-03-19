package platform;

/**
 * A delegate that is responsible for handling the specific property
 * @param <T> the type of property
 */
public interface IPropertyDelegate<T> {
	 
	T get();
	
	void addObserver(IPropertyObserver<T> observer);
	
	void removeObserver(IPropertyObserver<T> observer);

}

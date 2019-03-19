package platform;

public interface IPropertyObserver<T> {
	
	void onPropertyChanged(T oldValue, T newValue);
	
}

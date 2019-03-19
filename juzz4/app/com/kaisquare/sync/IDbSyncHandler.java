package com.kaisquare.sync;

import java.util.Collection;

public interface IDbSyncHandler<T> {
	
	boolean sync(String bucketName, Collection<T> data);

}

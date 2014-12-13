package com.cache;

import java.util.concurrent.*;

public class Cache implements ICache {
	private final ConcurrentHashMap<Long, Future<Object>> cache = new ConcurrentHashMap<>();
	private final Storage storage;
	
	public Cache(Storage storage) {
		this.storage = storage;
	}

	public Object get(Long key) {
		if (key == null) {
			throw new NullPointerException("Key must not be null");
		}
		Future<Object> resultFuture = cache.get(key);
		if (resultFuture == null) {
			resultFuture = loadIntoCacheIfAbsent(key);
		}
		return getResultFromCache(resultFuture);
	}

	private Future<Object> loadIntoCacheIfAbsent(Long key) {
		FutureTask<Object> resultTask = new FutureTask<>(() -> storage.load(key));
		Future<Object> resultFuture = cache.putIfAbsent(key, resultTask);
		if (resultFuture == null) {
            resultTask.run();
			return resultTask;
        }
		return resultFuture;
	}

	private Object getResultFromCache(Future<Object> resultFuture) {
		try {
			return resultFuture.get();
		} catch (InterruptedException | ExecutionException | NullPointerException e) {
			new RuntimeException(e).printStackTrace();
			System.exit(1);
			return false;
		}
	}
}

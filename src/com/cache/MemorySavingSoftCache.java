package com.cache;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

/**
 * This is supposed to be right cache implementation based on gc brain, but @see {com.cache.MemorySavingWeakCache}
 */
public class MemorySavingSoftCache implements ICache {
    private final ConcurrentHashMap<Long, SoftReference<Future<Object>>> cache = new ConcurrentHashMap<>();
    private final ReferenceQueue<Long> deathQueue = new ReferenceQueue<>();
    private final Storage storage;

    public MemorySavingSoftCache(Storage storage) {
        this.storage = storage;
    }

    public Object get(Long key) {
        if (key == null) {
            throw new NullPointerException("Key must not be null");
        }
        SoftReference<Future<Object>> resultFutureRef = cache.get(key);
        if (resultFutureRef == null || resultFutureRef.get() == null) {
            resultFutureRef = loadIntoCacheIfAbsent(key);
        }
        return getResultFromCache(resultFutureRef.get());
    }

    private SoftReference<Future<Object>> loadIntoCacheIfAbsent(Long key) {
        FutureTask<Object> resultTask = new FutureTask<>(() -> storage.load(key));
        SoftReference<Future<Object>> resultTaskRef = new SoftReference<>(resultTask);
        SoftReference<Future<Object>> resultFutureRef = cache.putIfAbsent(key, resultTaskRef);
        if (resultFutureRef == null || resultFutureRef.get() == null) {
            resultTask.run();
            return resultTaskRef;
        }
        return resultFutureRef;
    }

    private Object getResultFromCache(Future<Object> resultFuture) {
        try {
            return resultFuture.get();
        } catch (InterruptedException | ExecutionException | NullPointerException e) {
            e.printStackTrace();
            System.exit(1);
            return false;
        }
    }
}

package com.cache;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import static java.util.Collections.synchronizedMap;

public class StaleRemovedCache implements ICache {
    private static final int DEFAULT_MAX_SIZE = 128;
    private static final int DEFAULT_CAPACITY = 171;
    private final Map<Long, Object> cache;
    private final Storage storage;
    private final int maxCacheSize;

    public StaleRemovedCache(Storage storage) {
        this.cache = synchronizedMap(new LinkedHashMap<>(DEFAULT_CAPACITY, 0.75f, true));
        this.storage = storage;
        this.maxCacheSize = DEFAULT_MAX_SIZE;
    }

    public StaleRemovedCache(Storage storage, int maxCacheSize) {
        this.cache = synchronizedMap(new LinkedHashMap<>(maxCacheSize * 4 / 3, 0.75f, true));
        this.storage = storage;
        this.maxCacheSize = maxCacheSize;
    }

    public StaleRemovedCache(Storage storage, boolean accessOrder) {
        this.cache = synchronizedMap(new LinkedHashMap<>(171, 0.75f, accessOrder));
        this.storage = storage;
        this.maxCacheSize = 128;
    }

    public StaleRemovedCache(Storage storage, int maxCacheSize, boolean accessOrder) {
        this.cache = synchronizedMap(new LinkedHashMap<>(maxCacheSize * 4 / 3, 0.75f, accessOrder));
        this.storage = storage;
        this.maxCacheSize = maxCacheSize;
    }

    public Object get(Long key) {
        if (key == null) {
            throw new NullPointerException("Key must not be null");
        }
        Object result = cache.get(key);
        if (result == null) {
            return loadIntoCacheIfAbsent(key);
        }
        return result;
    }

    private Object loadIntoCacheIfAbsent(Long key) {
        synchronized (cache) {
            Object result = cache.get(key);
            if (result == null) {
                Object newResult = storage.load(key);
                cache.put(key, newResult);
                cleanStale();
                return newResult;
            }
            return result;
        }
    }

    // must be called from synchronized on cache context
    private void cleanStale() {
        if (cache.size() > maxCacheSize) {
            Set<Long> keySet = cache.keySet();
            Iterator<Long> iterator = keySet.iterator();
            iterator.next();
            iterator.remove();
            assert cache.size() <= maxCacheSize;
        }
    }
}
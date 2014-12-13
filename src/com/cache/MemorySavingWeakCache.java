package com.cache;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * It's said by smart people, that soft references are not actually good way to implement caches.
 * But other smart people say opposite.
 * Let me explain it for BTW most popular HotSpot JRE:
 * GC decides to remove soft reference according to the value of the timestamp field in SoftReference class,
 * which is populated with the time, when referent was last accessed, and to amount of free memory allocated for JVM.
 * It means that when memory consuming having a spike soft references are going to be recycled by gc, which is BTW
 * very hard task for gc, even if the memory consumption significantly decreases right after the spike.
 * Another important case is that memory spike causes not only the oldest entries to be removed from cache, but almost
 * all of them.
 * It means that SoftReference-based cache in most cases causes more harm than good.
 * So here is a WeakReference-based cache.
 *
 * Note: my experiments are showing that in random access mode
 * soft-ref-based cache behaves much better than weak-ref-based one
 */
public class MemorySavingWeakCache implements ICache {
    // Note, that 0-127 are never removed from cache
    private final Map<Long, Object> cache = new WeakHashMap<>();
    private final Storage storage;

    public MemorySavingWeakCache(Storage storage) {
        this.storage = storage;
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
                return newResult;
            }
            return result;
        }
    }
}

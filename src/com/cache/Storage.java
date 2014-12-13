package com.cache;

/**
 * Must be ThreadSafe
 */
public interface Storage {
	Object load(Long id);
}

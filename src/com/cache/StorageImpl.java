package com.cache;

import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

/**
 * ThreadSafe Stack confined
 */
public class StorageImpl implements Storage {
    @Override
    public Object load(Long id) {
//        try {
//            Thread.sleep(10);
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
        return new Loadable(id);
    }

    private static final class Loadable {
        private final char[] weight = new char[4096];
        private final long id;

        private Loadable(Long id) {
            this.id = id;
        }

        @Override
        public String toString() {
            return Arrays.toString(weight);
        }
    }

    public static void main(String[] args) throws InterruptedException {
        long testStartTime = System.nanoTime();
        ICache cache = new MemorySavingSoftCache(new StorageImpl());
        Runnable test = () -> {
            for (long i = 0; i < Long.MAX_VALUE; i++) {
                Object o = cache.get(i % 2_000_000);
                assertNotNull(o);
            }
        };
        Thread one = new Thread(test);
        one.start();
        Thread.sleep(2_000);
        Thread two = new Thread(test);
        two.start();
        Runnable randomTest = () -> {
            for (long i = 0; i < Long.MAX_VALUE; i++) {
                long key = ThreadLocalRandom.current().nextLong(2_000_000);
                // every 128 times
                if ((i & 127) == 0) {
                    long start = System.nanoTime();
                    Object obj = cache.get(key);
                    assertNotNull(obj);
                    // every 256 times
                    System.out.println(key + " got in thread " + Thread.currentThread().getId() +
                            " in " + (System.nanoTime() - start) + "ns");
                } else {
                    Object o = cache.get(key);
                    assertNotNull(o);
                }
            }
        };
        Thread.sleep(2_000);
        Thread three = new Thread(randomTest);
        three.start();
        one.join();
        two.join();
        three.join();
        System.out.println(TimeUnit.NANOSECONDS.toSeconds(System.nanoTime() - testStartTime) + "s");
    }

    private static void assertNotNull(Object o) {
        if (o == null) {
            throw new AssertionError("NULL");
        }
    }
}

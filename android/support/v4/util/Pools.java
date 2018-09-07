/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package android.support.v4.util;


import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

/**
 * Helper class for creating pools of objects. An example use looks like this:
 * <pre>
 * public class MyPooledClass {
 *
 *     private static final SynchronizedPool<MyPooledClass> sPool =
 *             new SynchronizedPool<MyPooledClass>(10);
 *
 *     public static MyPooledClass obtain() {
 *         MyPooledClass instance = sPool.acquire();
 *         return (instance != null) ? instance : new MyPooledClass();
 *     }
 *
 *     public void recycle() {
 *          // Clear state if needed.
 *          sPool.release(this);
 *     }
 *
 *     . . .
 * }
 * </pre>
 *
 */
public final class Pools {

    /**
     * Interface for managing a pool of objects.
     * <p><p> 管理一个对象池的接口
     * @param <T> The pooled type.池的大小
     */
    public interface Pool<T> {

        /**
         * @return 从池中获取一个对象。如果池为空，则返回null
         */
        @Nullable
        T acquire();

        /**
         * Release an instance to the pool.
         * <p><p>释放一个对象到池中
         * @param instance The instance to release.
         * @return Whether the instance was put in the pool.
         *
         * @throws IllegalStateException If the instance is already in the pool.
         */
        boolean release(@NonNull T instance);
    }

    private Pools() {
        /* do nothing - hiding constructor */
    }

    /**
     * 一个简单的对象池(非同步)。<p><p>Simple (non-synchronized) pool of objects.
     *
     * @param <T> The pooled type.
     */
    public static class SimplePool<T> implements Pool<T> {
        private final Object[] mPool;//对象池，被初始化后大小既定。

        private int mPoolSize;//池子当前持有对象的数量

        /**
         * Creates a new instance.
         * <p><p>构造一个对象池实例。
         * @param maxPoolSize The max pool size.
         *
         * @throws IllegalArgumentException 如果maxPoolSize小于等于0.
         */
        public SimplePool(int maxPoolSize) {
            if (maxPoolSize <= 0) {
                throw new IllegalArgumentException("The max pool size must be > 0");
            }
            mPool = new Object[maxPoolSize];
        }

        @Override
        @SuppressWarnings("unchecked")
        public T acquire() {
            if (mPoolSize > 0) {//如果池中数量大于0,取出最后一个。
                final int lastPooledIndex = mPoolSize - 1;
                T instance = (T) mPool[lastPooledIndex];
                mPool[lastPooledIndex] = null;
                mPoolSize--;
                return instance;
            }
            return null;
        }

        @Override
        public boolean release(@NonNull T instance) {
            if (isInPool(instance)) {//已经会收到池中了
                throw new IllegalStateException("Already in the pool!");
            }
            if (mPoolSize < mPool.length) {//如果池还没满，放入池中
                mPool[mPoolSize] = instance;
                mPoolSize++;
                return true;
            }
            return false;
        }
        /** instance 是否在池中 */
        private boolean isInPool(@NonNull T instance) {
            for (int i = 0; i < mPoolSize; i++) {
                if (mPool[i] == instance) {
                    return true;
                }
            }
            return false;
        }
    }

    /**
     * 同步的对象池。
     * <p><p>Synchronized) pool of objects.
     * @param <T> The pooled type.  对象池的类型
     */
    public static class SynchronizedPool<T> extends SimplePool<T> {
        private final Object mLock = new Object();

        /**
         * Creates a new instance.
         *
         * @param maxPoolSize The max pool size.
         *
         * @throws IllegalArgumentException If the max pool size is less than zero.
         */
        public SynchronizedPool(int maxPoolSize) {
            super(maxPoolSize);
        }

        @Override
        public T acquire() {
            synchronized (mLock) {
                return super.acquire();
            }
        }

        @Override
        public boolean release(@NonNull T element) {
            synchronized (mLock) {
                return super.release(element);
            }
        }
    }
}

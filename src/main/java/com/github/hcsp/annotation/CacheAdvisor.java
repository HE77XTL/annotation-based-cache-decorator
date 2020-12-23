package com.github.hcsp.annotation;

import net.bytebuddy.implementation.bind.annotation.AllArguments;
import net.bytebuddy.implementation.bind.annotation.Origin;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import net.bytebuddy.implementation.bind.annotation.SuperCall;
import net.bytebuddy.implementation.bind.annotation.This;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;

public class CacheAdvisor {
    private static ConcurrentHashMap<CacheKey, CacheValue> cache = new ConcurrentHashMap<>();

    @RuntimeType
    public static Object cache(@Origin Method method, @This Object thisObject, @AllArguments Object[] arguments, @SuperCall Callable<Object> superCall) throws Exception {
        CacheKey cacheKey = new CacheKey(thisObject, method.getName(), arguments);
        CacheValue resultInCache = cache.get(cacheKey);
        int cacheSeconds = method.getAnnotation(Cache.class).cacheSeconds();
        if (cacheValid(resultInCache, cacheSeconds)) {
            return resultInCache.value;
        } else {
            return invokeRealMethodAndPutIntoCache(superCall, cacheKey);
        }
    }

    private static Object invokeRealMethodAndPutIntoCache(Callable<Object> superCall, CacheKey cacheKey) throws Exception {
        Object realMethodInvocationResult = superCall.call();
        cache.put(cacheKey, new CacheValue(realMethodInvocationResult, System.currentTimeMillis()));
        return realMethodInvocationResult;
    }

    private static boolean cacheValid(CacheValue resultInCache, int cacheSeconds) {
        return resultInCache != null
                && (System.currentTimeMillis() - resultInCache.time) <= (cacheSeconds * 1000);
    }


    /**
     * 用来缓存的Map的Key
     */
    private static class CacheKey {
        private Object thisObject;
        private String methodName;
        private Object[] arguments;

        CacheKey(Object thisObject, String methodName, Object[] arguments) {
            this.thisObject = thisObject;
            this.methodName = methodName;
            this.arguments = arguments;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            CacheKey cacheKey = (CacheKey) o;
            return Objects.equals(thisObject, cacheKey.thisObject) &&
                    Objects.equals(methodName, cacheKey.methodName) &&
                    Arrays.equals(arguments, cacheKey.arguments);
        }

        @Override
        public int hashCode() {
            int result = Objects.hash(thisObject, methodName);
            result = 31 * result + Arrays.hashCode(arguments);
            return result;
        }
    }

    /**
     * 用来缓存的Map的value
     */
    private static class CacheValue {
        private Object value;
        private long time;

        CacheValue(Object value, long time) {
            this.value = value;
            this.time = time;
        }
    }
}

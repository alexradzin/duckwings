package org.duckwings;

import org.duckwings.internal.MethodComparator;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class ReflectionalWrapper<T, I> extends BaseWrapper<T, I> {
    private final Map<MethodKey, Optional<Method>> methodCache = new ConcurrentHashMap<>();

    ReflectionalWrapper(
            Class<I> face,
            @SuppressWarnings("OptionalUsedAsFieldOrParameterType") Optional<Function<Method, Throwable>> constructionFailure,
            @SuppressWarnings("OptionalUsedAsFieldOrParameterType") Optional<Function<Method, Throwable>> runtimeFailure) {
        super(face, constructionFailure, runtimeFailure);
    }


    protected Collection<Method> definedMethods(Object target) {
        Set<Method> targetMethods = new TreeSet<>(new MethodComparator());
        targetMethods.addAll(Arrays.stream(target.getClass().getMethods()).collect(Collectors.toList()));
        return targetMethods;
    }

    protected InvocationHandler createInvocationHandler(T target, Object ... others) {
        return new ReflectionalInvocationHandler<>(target, others);
    }

    private Optional<Method> targetMethod(Class<?> targetClass, Method method) {
        String methodName = method.getName();
        Class[] paramTypes = method.getParameterTypes();
        MethodKey key = new MethodKey(targetClass, methodName, paramTypes);
        Optional<Method> m = methodCache.computeIfAbsent(key, new Function<MethodKey, Optional<Method>>() {
            @Override
            public Optional<Method> apply(MethodKey methodKey) {
                for (Class<?> c = targetClass; c != null; c = c.getSuperclass()) {
                    try {
                        return Optional.of(c.getDeclaredMethod(methodName, method.getParameterTypes()));
                    } catch (NoSuchMethodException e) {
                        // ignore and try the next candidate
                    }
                }
                return Optional.empty();
            }
        });

        if (m.isPresent()) {
            return m;
        }

        runtimeFailure.ifPresent(methodThrowableFunction -> sneakyThrow(methodThrowableFunction.apply(method)));
        return Optional.empty();
    }

    private class ReflectionalInvocationHandler<T> implements InvocationHandler, Supplier<T> {
        private final T target;
        private final Object[] others;

        private ReflectionalInvocationHandler(T target, Object[] others) {
            this.target = target;
            this.others = others;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            Optional<Method> m = targetMethod(target.getClass(), method);
            if (m.isPresent()) {
                try {
                    return invoke(m.get(), target, args);
                } catch (ReflectiveOperationException e) {
                    // does not matter whether exception was thrown during invocation or during the method lookup:
                    // the decision whether throw exception of return default value is done in right after the if.
                }
            } else {
                for (Object obj : others) {
                    m = targetMethod(obj.getClass(), method);
                    if (m.isPresent()) {
                        try {
                            return invoke(m.get(), obj, args);
                        } catch (ReflectiveOperationException e) {
                            break;
                        }
                    }
                }
            }
            runtimeFailure.ifPresent(methodThrowableFunction -> sneakyThrow(methodThrowableFunction.apply(method)));
            return defaultValue.get(method.getReturnType());
        }

        private Object invoke(Method m, Object obj, Object[] args) throws InvocationTargetException, IllegalAccessException {
            m.setAccessible(true);
            return m.invoke(obj, args);
        }

        @Override
        public T get() {
            return target;
        }
    }


    static class MethodKey {
        private final Class<?> clazz;
        private final String methodName;
        private final Class[] paramTypes;

        MethodKey(Class<?> clazz, String methodName, Class[] paramTypes) {
            this.clazz = clazz;
            this.methodName = methodName;
            this.paramTypes = paramTypes;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            MethodKey methodKey = (MethodKey) o;
            return Objects.equals(clazz, methodKey.clazz) && Objects.equals(methodName, methodKey.methodName) && Arrays.equals(paramTypes, methodKey.paramTypes);
        }

        @Override
        public int hashCode() {
            return 31 * Objects.hash(clazz, methodName) + Arrays.hashCode(paramTypes);
        }
    }
}

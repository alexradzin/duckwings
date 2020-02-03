package org.duckwings;

import org.duckwings.internal.MethodComparator;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.TreeSet;
import java.util.function.Function;

abstract class BaseWrapper<T, I> implements Wrapper<T, I> {
    protected final Class<I> face;
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    protected final Optional<Function<Method, Throwable>> constructionFailure;
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    protected final Optional<Function<Method, Throwable>> runtimeFailure;

    protected static final Map<Class, Object> defaultValue = new HashMap<>();
    static {
        defaultValue.put(byte.class, (byte)0);
        defaultValue.put(short.class, (short)0);
        defaultValue.put(int.class, 0);
        defaultValue.put(long.class, 0L);
        defaultValue.put(char.class, (char)0);
        defaultValue.put(float.class, 0.0F);
        defaultValue.put(double.class, 0.0);
        defaultValue.put(boolean.class, false);
    };

    protected final Collection<Object> defaultValues;

    protected BaseWrapper(
            Class<I> face,
            @SuppressWarnings("OptionalUsedAsFieldOrParameterType") Optional<Function<Method, Throwable>> constructionFailure,
            @SuppressWarnings("OptionalUsedAsFieldOrParameterType") Optional<Function<Method, Throwable>> runtimeFailure) {
        this.face = face;
        this.constructionFailure = constructionFailure;
        this.runtimeFailure = runtimeFailure;

        Collection<Object> values = new ArrayList<>();
        values.add(null);
        values.addAll(defaultValue.values());
        defaultValues = values;
    }

    @Override
    public I wrap(T target, Object ... others) {
        if(constructionFailure.isPresent()) {
            Collection<Method> definedMethods = new TreeSet<>(new MethodComparator());
            definedMethods.addAll(definedMethods(target));
            Arrays.stream(others).map(this::definedMethods).forEach(definedMethods::addAll);
            for (Method m : face.getMethods()) {
                if (!definedMethods.contains(m)) {
                    sneakyThrow(constructionFailure.get().apply(m));
                }
            }
        }

        @SuppressWarnings("unchecked")
        I proxy = (I) Proxy.newProxyInstance(
                Thread.currentThread().getContextClassLoader(),
                new Class[] {face},
                createInvocationHandler(target, others));

        return proxy;
    }

    @Override
    public I unwrap(Object obj) {
        return DuckWings.unwrap(obj);
    }

    @SuppressWarnings("unchecked")
    protected <E extends Throwable> void sneakyThrow(Throwable e) throws E {
        throw (E) e;
    }

    protected abstract Collection<Method> definedMethods(Object target);
    protected abstract InvocationHandler createInvocationHandler(T target, Object ... others);
}

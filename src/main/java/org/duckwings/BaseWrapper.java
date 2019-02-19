package org.duckwings;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

abstract class BaseWrapper<T, I> implements Wrapper<T, I> {
    protected final Class<I> face;
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    protected final Optional<Function<Method, Throwable>> constructionFailure;
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    protected final Optional<Function<Method, Throwable>> runtimeFailure;

    protected final Map<Class, Object> defaultValue = new HashMap<Class, Object>() {{
        put(byte.class, (byte)0);
        put(short.class, (short)0);
        put(int.class, 0);
        put(long.class, 0L);
        put(char.class, (char)0);
        put(float.class, 0.0F);
        put(double.class, 0.0);
        put(boolean.class, false);
    }};

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
    public I wrap(T target) {
        if(constructionFailure.isPresent()) {
            Optional<Method> missingMethod = Arrays.stream(face.getMethods()).filter(m -> !definedMethods(target).contains(m)).findFirst();
            missingMethod.ifPresent(method -> sneakyThrow(constructionFailure.get().apply(method)));
        }

        @SuppressWarnings("unchecked")
        I proxy = (I) Proxy.newProxyInstance(
                Thread.currentThread().getContextClassLoader(),
                new Class[] {face},
                createInvocationHandler(target));

        return proxy;
    }

    @SuppressWarnings("unchecked")
    protected <E extends Throwable> void sneakyThrow(Throwable e) throws E {
        throw (E) e;
    }

    protected abstract Collection<Method> definedMethods(T target);
    protected abstract InvocationHandler createInvocationHandler(T target);
}

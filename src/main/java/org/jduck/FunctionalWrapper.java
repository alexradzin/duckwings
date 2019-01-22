package org.jduck;

import org.jduck.internal.MethodComparator;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.function.BiFunction;
import java.util.function.Function;

public class FunctionalWrapper<T, I> implements Wrapper<T, I> {
    private final Class<I> face;
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private final Optional<Function<Method, Throwable>> constructionFailure;
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private final Optional<Function<Method, Throwable>> runtimeFailure;

    private final Map<Class, Object> defaultValue = new HashMap<Class, Object>() {{
        put(byte.class, 0);
        put(short.class, 0);
        put(int.class, 0);
        put(long.class, 0);
        put(char.class, 0);
        put(float.class, 0.0F);
        put(double.class, 0.0);
        put(boolean.class, false);
    }};


    private Map<Method, Object> functions = new TreeMap<>(new MethodComparator());

    public FunctionalWrapper(
            Class<I> face,
            @SuppressWarnings("OptionalUsedAsFieldOrParameterType") Optional<Function<Method, Throwable>> constructionFailure,
            @SuppressWarnings("OptionalUsedAsFieldOrParameterType") Optional<Function<Method, Throwable>> runtimeFailure) {
        this.face = face;
        this.constructionFailure = constructionFailure;
        this.runtimeFailure = runtimeFailure;
    }


    public FunctionalWrapper<T, I> using(Function<I, Object> facefunc, Function<T, Object> classfunc) {
        facefunc.apply(functionCollectingProxy(classfunc));
        return this;
    }


    public <P> FunctionalWrapper<T, I> using(BiFunction<I, Object, Object> facefunc, BiFunction<T, P, Object> classfunc) {
        facefunc.apply(functionCollectingProxy(classfunc), null);
        return this;
    }

    private I functionCollectingProxy(Object classfunc) {
        @SuppressWarnings("unchecked")
        I proxy = (I)Proxy.newProxyInstance(
                Thread.currentThread().getContextClassLoader(),
                new Class[]{face},
                (p, method, args) -> {
                    functions.put(method, classfunc);
                    return defaultValue.get(method.getReturnType());
                });

        return proxy;
    }



    @Override
    public I wrap(T target) {
        if(constructionFailure.isPresent()) {
            Optional<Method> missingMethod = Arrays.stream(face.getMethods()).filter(m -> !functions.containsKey(m)).findFirst();
            missingMethod.ifPresent(method -> sneakyThrow(constructionFailure.get().apply(method)));
        }

        @SuppressWarnings("unchecked")
        I proxy = (I) Proxy.newProxyInstance(
                Thread.currentThread().getContextClassLoader(),
                new Class[] {face},
                new FunctionalInvocationHandler(target));

        return proxy;
    }


    private class FunctionalInvocationHandler implements InvocationHandler {
        private final T target;

        private FunctionalInvocationHandler(T target) {
            this.target = target;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {
            return invoker(method).apply(args);
        }
        @SuppressWarnings("unchecked")
        private Function<Object[], Object> invoker(Method method) {
            return args -> {
                Object function = functions.get(method);
                if(function instanceof Function) {
                    return ((Function)function).apply(target);
                } else if (function instanceof BiFunction) {
                    return ((BiFunction)function).apply(target, args.length >= 1 ? args[0] : null);
                }
                runtimeFailure.ifPresent(methodThrowableFunction -> sneakyThrow(methodThrowableFunction.apply(method)));
                return defaultValue.get(method.getReturnType());
            };
        }
    }


    @SuppressWarnings("unchecked")
    public static <E extends Throwable> void sneakyThrow(Throwable e) throws E {
        throw (E) e;
    }
}

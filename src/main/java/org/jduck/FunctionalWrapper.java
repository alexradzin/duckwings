package org.jduck;

import org.jduck.internal.MethodComparator;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.function.BiFunction;
import java.util.function.Function;

public class FunctionalWrapper<T, I> extends BaseWrapper<T, I> {
    private Map<Method, Object> functions = new TreeMap<>(new MethodComparator());

    FunctionalWrapper(
            Class<I> face,
            @SuppressWarnings("OptionalUsedAsFieldOrParameterType") Optional<Function<Method, Throwable>> constructionFailure,
            @SuppressWarnings("OptionalUsedAsFieldOrParameterType") Optional<Function<Method, Throwable>> runtimeFailure) {
        super(face, constructionFailure, runtimeFailure);
    }


    public FunctionalWrapper<T, I> using(Function<I, Object> facefunc, Function<T, Object> classfunc) {
        facefunc.apply(functionCollectingProxy(classfunc));
        return this;
    }


    public <P, R> FunctionalWrapper<T, I> using(BiFunction<I, P, R> facefunc, BiFunction<T, P, R> classfunc) {
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
    protected Collection<Method> definedMethods(T target) {
        return functions.keySet();
    }

    @Override
    protected InvocationHandler createInvocationHandler(T target) {
        return new FunctionalInvocationHandler(target);
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
}

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
    private Map<Method, FunctionContainer<?>> functions = new TreeMap<>(new MethodComparator());

    FunctionalWrapper(
            Class<I> face,
            @SuppressWarnings("OptionalUsedAsFieldOrParameterType") Optional<Function<Method, Throwable>> constructionFailure,
            @SuppressWarnings("OptionalUsedAsFieldOrParameterType") Optional<Function<Method, Throwable>> runtimeFailure) {
        super(face, constructionFailure, runtimeFailure);
    }


    public FunctionalWrapper<T, I> using(Function<I, Object> facefunc, Function<T, Object> classfunc) {
        facefunc.apply(functionCollectingProxy(classfunc, NoArgFunctionContainer::new));
        return this;
    }


    @SuppressWarnings("LambdaBodyCanBeCodeBlock")
    public <P, R> FunctionalWrapper<T, I> using(BiFunction<I, P, R> facefunc, BiFunction<T, P, R> classfunc) {
//        facefunc.apply(functionCollectingProxy(classfunc, OneArgFunctionContainer::new), null); // for some reason labda does not work
        facefunc.apply(functionCollectingProxy(classfunc, f -> new OneArgFunctionContainer<>(f)), null);
        return this;
    }

    private <F> I functionCollectingProxy(F classfunc, Function<F, FunctionContainer> containerFactory) {
        @SuppressWarnings("unchecked")
        I proxy = (I)Proxy.newProxyInstance(
                Thread.currentThread().getContextClassLoader(),
                new Class[]{face},
                (p, method, args) -> {
                    functions.put(method, containerFactory.apply(classfunc));
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
                FunctionContainer<?> container = functions.get(method);
                if (container != null) {
                    return container.eval(target, args);
                }

                runtimeFailure.ifPresent(methodThrowableFunction -> sneakyThrow(methodThrowableFunction.apply(method)));
                return defaultValue.get(method.getReturnType());
            };
        }
    }

    private abstract class FunctionContainer<F> {
        protected final F function;

        public FunctionContainer(F function) {
            this.function = function;
        }

        public abstract Object eval(T target, Object[] args);
    }

    private class NoArgFunctionContainer extends FunctionContainer<Function<T, Object>> {
        public NoArgFunctionContainer(Function<T, Object> function) {
            super(function);
        }

        @Override
        public Object eval(T target, Object[] args) {
            return function.apply(target);
        }
    }

    private class OneArgFunctionContainer<P, R> extends FunctionContainer<BiFunction<T, P, R>> {
        public OneArgFunctionContainer(BiFunction<T, P, R> function) {
            super(function);
        }

        @Override
        public Object eval(T target, Object[] args) {
            @SuppressWarnings("unchecked")
            P arg = (P)args[0];
            return function.apply(target, arg);
        }
    }


}

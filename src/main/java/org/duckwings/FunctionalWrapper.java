package org.duckwings;

import org.duckwings.internal.MethodComparator;
import org.duckwings.internal.TetraFunction;
import org.duckwings.internal.TriFunction;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

public class FunctionalWrapper<T, I> extends BaseWrapper<T, I> {
    private Map<Method, FunctionContainer<?>> functions = new TreeMap<>(new MethodComparator());
    private Wrapper<T, I> fallback;
    private Wrapper[] otherWrappers;

    FunctionalWrapper(
            Class<I> face,
            @SuppressWarnings("OptionalUsedAsFieldOrParameterType") Optional<Function<Method, Throwable>> constructionFailure,
            @SuppressWarnings("OptionalUsedAsFieldOrParameterType") Optional<Function<Method, Throwable>> runtimeFailure) {
        super(face, constructionFailure, runtimeFailure);
    }


    public FunctionalWrapper<T, I> using(Function<I, Object> facefunc, Function<T, Object> classfunc) {
        facefunc.apply(functionCollectingProxy(classfunc, f -> new NoArgFunctionContainer((Function<Object, Object>)f)));
        return this;
    }


    @SuppressWarnings("unchecked")
    public <P, R> FunctionalWrapper<T, I> using(BiFunction<I, P, R> facefunc, BiFunction<T, P, R> classfunc) {
//        facefunc.apply(functionCollectingProxy(classfunc, OneArgFunctionContainer::new), null); // for some reason labda does not work
        boolean found = false;
        for (Object value : defaultValues) {
            try {
                facefunc.apply(functionCollectingProxy(classfunc, f -> new OneArgFunctionContainer(f)), (P)value);
                found = true;
                break;
            } catch (NullPointerException | ClassCastException e) {
                // ignore exception. Try the next candidate for  default value
            }
        }

        return assertFound(found);
    }


    @SuppressWarnings("unchecked")
    public <P1, P2, R> FunctionalWrapper<T, I> using(TriFunction<I, P1, P2, R> facefunc, TriFunction<T, P1, P2, R>  classfunc) {
        boolean found = false;
        for (Object value1 : defaultValues) {
            for (Object value2 : defaultValues) {
                try {
                    facefunc.apply(functionCollectingProxy(classfunc, f -> new TwoArgFunctionContainer(f)), (P1)value1, (P2)value2);
                    found = true;
                    break;
                } catch (NullPointerException | ClassCastException e) {
                    // ignore exception. Try the next candidate for  default value
                }
            }
        }

        return assertFound(found);
    }

    @SuppressWarnings("unchecked")
    public <P1, P2, P3, R> FunctionalWrapper<T, I> using(TetraFunction<I, P1, P2, P3, R> facefunc, TetraFunction<T, P1, P2, P3, R>  classfunc) {
        boolean found = false;
        for (Object value1 : defaultValues) {
            for (Object value2 : defaultValues) {
                for (Object value3 : defaultValues) {
                    try {
                        facefunc.apply(functionCollectingProxy(classfunc, f -> new ThreeArgFunctionContainer(f)), (P1)value1, (P2)value2, (P3)value3);
                        found = true;
                        break;
                    } catch (NullPointerException | ClassCastException e) {
                        // ignore exception. Try the next candidate for  default value
                    }
                }
            }
        }

        return assertFound(found);
    }

    public FunctionalWrapper<T, I> fallback(Wrapper<T, I> fallback) {
        this.fallback = fallback;
        return this;
    }

    public FunctionalWrapper<T, I> with(Wrapper ... others) {
        this.otherWrappers = others;
        return this;
    }

    private FunctionalWrapper<T, I> assertFound(boolean found) {
        if (!found) {
            throw new IllegalArgumentException("Cannot locate compatible function");
        }

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
    protected Collection<Method> definedMethods(Object target) {
        return functions.keySet();
    }

    @Override
    protected InvocationHandler createInvocationHandler(T target, Object ... others) {
        return new FunctionalInvocationHandler(target, others);
    }

    private class FunctionalInvocationHandler implements InvocationHandler, Supplier<T> {
        private final T target;
        private final I fb;
        private final Object[] others;

        private FunctionalInvocationHandler(T target, Object[] others) {
            this.target = target;
            fb = fallback == null ? null : fallback.wrap(target);
            this.others = others;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {
            return invoker(method).apply(args);
        }

        @SuppressWarnings("unchecked")
        private Function<Object[], Object> invoker(Method method) {
            return args -> {
                FunctionContainer<?> container = functions.get(method);
                try {
                    if (container != null) {
                        return container.eval(target, args);
                    } else {
                        int n = others.length;
                        for (int i = 0; i < n ; i++) {
                            Wrapper w = FunctionalWrapper.this.otherWrappers[i];
                            if (w instanceof FunctionalWrapper && ((FunctionalWrapper)w).functions.containsKey(method)) {
                                FunctionContainer<?> container2 = (FunctionContainer<?>)((FunctionalWrapper)w).functions.get(method);
                                return container2.eval(others[i], args);
                            }
                        }

                        if (fallback != null) {
                            return Proxy.getInvocationHandler(fb).invoke(fb, method, args);
                        }
                    }
                } catch (Throwable t) {
                    // does not matter whether exception was thrown during invocation or during the method lookup:
                    // the decision whether throw exception of return default value is done in right after the if.
                }

                runtimeFailure.ifPresent(methodThrowableFunction -> sneakyThrow(methodThrowableFunction.apply(method)));
                return defaultValue.get(method.getReturnType());
            };
        }

        @Override
        public T get() {
            return target;
        }
    }

    private abstract class FunctionContainer<F> {
        protected final F function;

        FunctionContainer(F function) {
            this.function = function;
        }

        protected abstract Object eval(Object target, Object[] args);
    }

    private class NoArgFunctionContainer extends FunctionContainer<Function<Object, Object>> {
        NoArgFunctionContainer(Function<Object, Object> function) {
            super(function);
        }

        @Override
        protected Object eval(Object target, Object[] args) {
            return function.apply(target);
        }
    }

    private class OneArgFunctionContainer<P, R> extends FunctionContainer<BiFunction<Object, P, R>> {
        OneArgFunctionContainer(BiFunction<Object, P, R> function) {
            super(function);
        }

        @Override
        protected Object eval(Object target, Object[] args) {
            @SuppressWarnings("unchecked")
            P arg = (P)args[0];
            return function.apply(target, arg);
        }
    }

    private class TwoArgFunctionContainer<P1, P2, R> extends FunctionContainer<TriFunction<Object, P1, P2, R>> {
        TwoArgFunctionContainer(TriFunction<Object, P1, P2, R> function) {
            super(function);
        }

        @Override
        protected Object eval(Object target, Object[] args) {
            @SuppressWarnings("unchecked") P1 arg1 = (P1)args[0];
            @SuppressWarnings("unchecked") P2 arg2 = (P2)args[1];
            return function.apply(target, arg1, arg2);
        }
    }

    private class ThreeArgFunctionContainer<P1, P2, P3, R> extends FunctionContainer<TetraFunction<Object, P1, P2, P3, R>> {
        ThreeArgFunctionContainer(TetraFunction<Object, P1, P2, P3, R>function) {
            super(function);
        }

        @Override
        protected Object eval(Object target, Object[] args) {
            @SuppressWarnings("unchecked") P1 arg1 = (P1)args[0];
            @SuppressWarnings("unchecked") P2 arg2 = (P2)args[1];
            @SuppressWarnings("unchecked") P3 arg3 = (P3)args[2];
            return function.apply(target, arg1, arg2, arg3);
        }
    }
}

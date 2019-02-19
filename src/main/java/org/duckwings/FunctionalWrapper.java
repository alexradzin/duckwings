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


    @SuppressWarnings("unchecked")
    public <P, R> FunctionalWrapper<T, I> using(BiFunction<I, P, R> facefunc, BiFunction<T, P, R> classfunc) {
//        facefunc.apply(functionCollectingProxy(classfunc, OneArgFunctionContainer::new), null); // for some reason labda does not work
        boolean found = false;
        for (Object value : defaultValues) {
            try {
                facefunc.apply(functionCollectingProxy(classfunc, f -> new OneArgFunctionContainer<>(f)), (P)value);
                found = true;
                break;
            } catch (NullPointerException | ClassCastException e) {
                // ignore exception. Try the next candidate for  default value
            }
        }

        if (!found) {
            throw new IllegalArgumentException("Cannot locate compatible function");
        }

        return this;
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

        if (!found) {
            throw new IllegalArgumentException("Cannot locate compatible function");
        }


        return this;
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

        FunctionContainer(F function) {
            this.function = function;
        }

        abstract Object eval(T target, Object[] args);
    }

    private class NoArgFunctionContainer extends FunctionContainer<Function<T, Object>> {
        NoArgFunctionContainer(Function<T, Object> function) {
            super(function);
        }

        @Override
        Object eval(T target, Object[] args) {
            return function.apply(target);
        }
    }

    private class OneArgFunctionContainer<P, R> extends FunctionContainer<BiFunction<T, P, R>> {
        OneArgFunctionContainer(BiFunction<T, P, R> function) {
            super(function);
        }

        @Override
        Object eval(T target, Object[] args) {
            @SuppressWarnings("unchecked")
            P arg = (P)args[0];
            return function.apply(target, arg);
        }
    }

    private class TwoArgFunctionContainer<P1, P2, R> extends FunctionContainer<TriFunction<T, P1, P2, R>> {
        TwoArgFunctionContainer(TriFunction<T, P1, P2, R> function) {
            super(function);
        }

        @Override
        Object eval(T target, Object[] args) {
            @SuppressWarnings("unchecked") P1 arg1 = (P1)args[0];
            @SuppressWarnings("unchecked") P2 arg2 = (P2)args[1];
            return function.apply(target, arg1, arg2);
        }
    }

    private class ThreeArgFunctionContainer<P1, P2, P3, R> extends FunctionContainer<TetraFunction<T, P1, P2, P3, R>> {
        ThreeArgFunctionContainer(TetraFunction<T, P1, P2, P3, R>function) {
            super(function);
        }

        @Override
        Object eval(T target, Object[] args) {
            @SuppressWarnings("unchecked") P1 arg1 = (P1)args[0];
            @SuppressWarnings("unchecked") P2 arg2 = (P2)args[1];
            @SuppressWarnings("unchecked") P3 arg3 = (P3)args[2];
            return function.apply(target, arg1, arg2, arg3);
        }
    }
}

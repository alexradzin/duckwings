package org.duckwings;

import org.duckwings.internal.MethodComparator;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class ReflectionalWrapper<T, I> extends BaseWrapper<T, I> {
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
        try {
            return Optional.of(targetClass.getMethod(method.getName(), method.getParameterTypes()));
        } catch (NoSuchMethodException e) {
            runtimeFailure.ifPresent(methodThrowableFunction -> sneakyThrow(methodThrowableFunction.apply(method)));
            return Optional.empty();
        }
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
                    return m.get().invoke(target, args);
                } catch (ReflectiveOperationException e) {
                    // does not matter whether exception was thrown during invocation or during the method lookup:
                    // the decision whether throw exception of return default value is done in right after the if.
                }
            } else {
                for (Object obj : others) {
                    m = targetMethod(obj.getClass(), method);
                    if (m.isPresent()) {
                        try {
                            return m.get().invoke(obj, args);
                        } catch (ReflectiveOperationException e) {
                            break;
                        }
                    }
                }
            }
            runtimeFailure.ifPresent(methodThrowableFunction -> sneakyThrow(methodThrowableFunction.apply(method)));
            return defaultValue.get(method.getReturnType());
        }

        @Override
        public T get() {
            return target;
        }
    }
}

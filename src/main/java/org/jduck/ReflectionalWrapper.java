package org.jduck;

import org.jduck.internal.MethodComparator;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ReflectionalWrapper<T, I> extends BaseWrapper<T, I> {
    ReflectionalWrapper(
            Class<I> face,
            @SuppressWarnings("OptionalUsedAsFieldOrParameterType") Optional<Function<Method, Throwable>> constructionFailure,
            @SuppressWarnings("OptionalUsedAsFieldOrParameterType") Optional<Function<Method, Throwable>> runtimeFailure) {
        super(face, constructionFailure, runtimeFailure);
    }


    protected Collection<Method> definedMethods(T target) {
        Set<Method> targetMethods = new TreeSet<>(new MethodComparator());
        targetMethods.addAll(Arrays.stream(target.getClass().getMethods()).collect(Collectors.toList()));
        return targetMethods;
    }

    protected InvocationHandler createInvocationHandler(T target) {
        return (proxy, method, args) -> {
            Optional<Method> m = targetMethod(target.getClass(), method);
            if (m.isPresent()) {
                try {
                    return m.get().invoke(target, args);
                } catch (ReflectiveOperationException e) {
                    // does not matter whether exception was thrown during invocation or during the method lookup:
                    // the decision whether throw exception of return default value is done in right after the if.
                }
            }
            runtimeFailure.ifPresent(methodThrowableFunction -> sneakyThrow(methodThrowableFunction.apply(method)));
            return defaultValue.get(method.getReturnType());
        };
    }



    private Optional<Method> targetMethod(Class<?> targetClass, Method method) {
        try {
            return Optional.of(targetClass.getMethod(method.getName(), method.getParameterTypes()));
        } catch (NoSuchMethodException e) {
            runtimeFailure.ifPresent(methodThrowableFunction -> sneakyThrow(methodThrowableFunction.apply(method)));
            return Optional.empty();
        }
    }
}

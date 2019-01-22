package org.jduck;

import org.jduck.internal.MethodComparator;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ReflectionalWrapper<T, I> implements Wrapper<T, I> {
    private final Class<?> face;
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private final Optional<Function<Method, Throwable>> constructionFailure;
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private final Optional<Function<Method, Throwable>> runtimeFailure;
    private Function<Method, Exception> ef;

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

    public ReflectionalWrapper(
            Class<I> face,
            @SuppressWarnings("OptionalUsedAsFieldOrParameterType") Optional<Function<Method, Throwable>> constructionFailure,
            @SuppressWarnings("OptionalUsedAsFieldOrParameterType") Optional<Function<Method, Throwable>> runtimeFailure) {
        this.face = face;
        this.constructionFailure = constructionFailure;
        this.runtimeFailure = runtimeFailure;
    }

    @SuppressWarnings("unchecked")
    @Override
    public I wrap(T target) {
        if(constructionFailure.isPresent()) {
            Set<Method> targetMethods = new TreeSet<>(new MethodComparator());
            targetMethods.addAll(Arrays.stream(target.getClass().getMethods()).collect(Collectors.toList()));
            Optional<Method> missingMethod = Arrays.stream(face.getMethods()).filter(m -> !targetMethods.contains(m)).findFirst();
            missingMethod.ifPresent(method -> sneakyThrow(constructionFailure.get().apply(method)));
        }

        return (I)Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(), new Class[]{face}, (proxy, method, args) -> {
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
        });
    }


    private Optional<Method> targetMethod(Class<?> targetClass, Method method) {
        try {
            return Optional.of(targetClass.getMethod(method.getName(), method.getParameterTypes()));
        } catch (NoSuchMethodException e) {
            runtimeFailure.ifPresent(methodThrowableFunction -> sneakyThrow(methodThrowableFunction.apply(method)));
            return Optional.empty();
        }
    }

    @SuppressWarnings("unchecked")
    public static <E extends Throwable> void sneakyThrow(Throwable e) throws E {
        throw (E) e;
    }

}

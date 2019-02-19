package org.duckwings;


import java.lang.reflect.Method;
import java.util.Optional;
import java.util.function.Function;

public class DuckWings<T, I> {
    public static WrapperBuilder builder() {
        return new WrapperBuilder();
    }

    public static class WrapperBuilder {
        @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
        private Optional<Function<Method, Throwable>> constructionFailure = Optional.empty();
        @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
        private Optional<Function<Method, Throwable>> runtimeFailure = Optional.empty();


        public WrapperBuilder throwIfAbsentDuringBuilding(Function<Method, Throwable> constructionFailure) {
            this.constructionFailure = Optional.of(constructionFailure);
            return this;
        }

        public WrapperBuilder throwIfAbsentAtRuntime(Function<Method, Throwable> runtimeFailure) {
            this.runtimeFailure = Optional.of(runtimeFailure);
            return this;
        }

        public <T, I> FunctionalWrapper<T, I> functional(Class<I> faceType, Class<T> targetType) {
            return new FunctionalWrapper<>(faceType, constructionFailure, runtimeFailure);
        }

        public <T, I> Wrapper<T, I> reflect(Class<I> faceType) {
            return new ReflectionalWrapper<>(faceType, constructionFailure, runtimeFailure);
        }
    }
}

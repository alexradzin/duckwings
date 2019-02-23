package org.duckwings.internal;

/**
 * Similar to {@link java.util.function.BiFunction} but accepts 3 arguments.
 */
@FunctionalInterface
public interface TriFunction<A, B, C, R> {
    R apply(A a, B b, C c);
}

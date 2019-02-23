package org.duckwings.internal;

/**
 * Similar to {@link java.util.function.BiFunction} but accepts 4 arguments.
 */
@FunctionalInterface
public interface TetraFunction<A, B, C, D, R> {
    R apply(A a, B b, C c, D d);
}

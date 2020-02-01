package org.duckwings;

public interface Wrapper<T, I> {
    I wrap(T obj);
    I unwrap(Object obj);
}

package org.duckwings;

public interface Wrapper<T, I> {
    I wrap(T obj);
}

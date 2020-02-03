package org.duckwings;

public interface Wrapper<T, I> {
    I wrap(T obj, Object ... others);
    I unwrap(Object obj);
}

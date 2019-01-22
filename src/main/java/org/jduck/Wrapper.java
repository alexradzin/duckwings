package org.jduck;

public interface Wrapper<T, I> {
    I wrap(T obj);
}

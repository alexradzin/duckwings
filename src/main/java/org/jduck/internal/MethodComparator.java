package org.jduck.internal;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Objects;

public class MethodComparator implements Comparator<Method> {
    @Override
    public int compare(Method m1, Method m2) {
        return hash(m1) - hash(m2);
    }

    private int hash(Method method) {
        return Objects.hash(method.getReturnType(), method.getName(), Arrays.asList(method.getParameterTypes()));
    }
}

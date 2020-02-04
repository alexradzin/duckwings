package org.duckwings;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class MethodKeyTest {
    @Test
    void equal() {
        assertEquals(new ReflectionalWrapper.MethodKey(String.class, "toString", new Class[0]), new ReflectionalWrapper.MethodKey(String.class, "toString", new Class[0]));
        assertEquals(new ReflectionalWrapper.MethodKey(String.class, "charAt", new Class[] {int.class}), new ReflectionalWrapper.MethodKey(String.class, "charAt", new Class[] {int.class}));
        assertEquals(new ReflectionalWrapper.MethodKey(String.class, "substing", new Class[] {int.class, int.class}), new ReflectionalWrapper.MethodKey(String.class, "substing", new Class[] {int.class, int.class}));
    }

    @Test
    void notEqual() {
        assertNotEquals(new ReflectionalWrapper.MethodKey(String.class, "toString", new Class[0]), new ReflectionalWrapper.MethodKey(Object.class, "toString", new Class[0]));
        assertNotEquals(new ReflectionalWrapper.MethodKey(String.class, "toString", new Class[0]), new ReflectionalWrapper.MethodKey(String.class, "toLowerCase", new Class[0]));
        assertNotEquals(new ReflectionalWrapper.MethodKey(String.class, "substring", new Class[] {int.class}), new ReflectionalWrapper.MethodKey(String.class, "substring", new Class[] {int.class, int.class}));
    }
}

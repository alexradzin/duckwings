package org.jduck;

import org.junit.jupiter.api.Test;

import java.lang.reflect.UndeclaredThrowableException;
import java.util.ArrayList;
import java.util.Collection;

import static java.lang.String.format;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JDuckTest {
    @Test
    void reflectiveStringAsCharSequenceLength() {
       assertEquals("hello".length(), JDuck.builder().reflect(CharSequence.class).wrap("hello").length());
    }

    @Test
    void reflectiveStringAsLength() {
        assertEquals("world".length(), JDuck.builder().reflect(Length.class).wrap("world").length());
    }

    @Test
    void functionalString() {
        assertEquals("function".length(), JDuck.builder().functional(Length.class, String.class).using(Length::length, String::length).wrap("function").length());
    }

    @Test
    void functionalStringAsCollectionLength() {
        assertEquals("function".length(), JDuck.builder().functional(Collection.class, String.class).using(Collection::size, String::length).wrap("function").size());
    }

    @Test
    void functionalStringAsCollection() {

        Wrapper<String, Collection> wrapper = JDuck.builder().functional(Collection.class, String.class)
                .using(Collection::size, String::length)
                .using(Collection::isEmpty, String::isEmpty)
                .using(Collection::contains, String::contains);

        String str = "function rocks";
        Collection collectionOverString = wrapper.wrap(str);

        assertEquals(str.length(), collectionOverString.size());
        assertFalse(collectionOverString.isEmpty());
        assertTrue(wrapper.wrap("").isEmpty());
        assertTrue(collectionOverString.contains("f"));
        assertFalse(collectionOverString.contains("Z"));
    }


    @Test
    void throwIfAbsentDuringBuildingWhenMethodIsMissing() {
        NoSuchMethodException e = assertThrows(
                NoSuchMethodException.class,
                () -> JDuck.builder()
                    .throwIfAbsentDuringBuilding((m) -> new NoSuchMethodException(format("Method %s does not exist", m.getName())))
                    .reflect(Length.class).wrap(new ArrayList<>())
        );
        assertEquals("Method length does not exist", e.getMessage());
    }


    @Test
    void throwIfAbsentDuringBuildingWhenMethodExists() {
        JDuck.builder()
                .throwIfAbsentDuringBuilding((m) -> new NoSuchMethodException(format("Method %s does not exist", m.getName())))
                .reflect(Length.class).wrap(""); // method length exists in class String
    }

    @Test
    void throwIfAbsentAtRuntimeWhenMethodIsMissing() {
        // Length does not exist in List but this line should not cause exception because the method is validated at runtime only
        Length wrappedLength = JDuck.builder()
                .throwIfAbsentAtRuntime((m) -> new NoSuchMethodException(format("Method %s does not exist", m.getName())))
                .reflect(Length.class).wrap(new ArrayList<>());

        UndeclaredThrowableException e = assertThrows(UndeclaredThrowableException.class, wrappedLength::length);
        assertEquals("Method length does not exist", e.getCause().getMessage());
    }


    @Test
    void throwIfAbsentAtRuntimeWhenMethodExists() {
        assertEquals("hello".length(),
                JDuck.builder()
                .throwIfAbsentAtRuntime((m) -> new NoSuchMethodException(format("Method %s does not exist", m.getName())))
                .reflect(Length.class).wrap("hello").length());
    }


    @Test
    void functionalStringAddFunctions() {
        Wrapper<String, ExtendedString> wrapper = JDuck.builder().functional(ExtendedString.class, String.class)
                .using(ExtendedString::toInt, Integer::parseInt)
                .using(ExtendedString::toLong, Long::parseLong)
                .using(ExtendedString::toBoolean, Boolean::parseBoolean)
                .using(ExtendedString::startsWithIgnoreCase, (s, s2) -> s.toLowerCase().startsWith(s2.toLowerCase()))
                .using(ExtendedString::substring, String::substring);



        assertEquals(5, wrapper.wrap("5").toInt());
        long now = System.currentTimeMillis();
        assertEquals(now, wrapper.wrap("" + now).toLong());
        assertTrue(wrapper.wrap("true").toBoolean());
        assertFalse(wrapper.wrap("false").toBoolean());
        assertTrue(wrapper.wrap("Hello").startsWithIgnoreCase("hell"));
        assertFalse(wrapper.wrap("Hello").startsWithIgnoreCase("heaven"));
        assertEquals("Hello".substring(2, 3), wrapper.wrap("Hello").substring(2, 3));
    }


    @Test
    void functionalStringAddFunctionsTwoArgs() {
        Wrapper<String, ExtendedString> wrapper = JDuck.builder().functional(ExtendedString.class, String.class)
                .using(ExtendedString::substring, String::substring);

        assertEquals("Hello".substring(2, 3), wrapper.wrap("Hello").substring(2, 3));
    }

    @Test
    void functionalStringAddFunctionsWithDefaultValue() {
        Wrapper<String, StrangeOperations> wrapper = JDuck.builder().functional(StrangeOperations.class, String.class)
                .using(StrangeOperations::tail, s -> s.substring(s.length() - 10));
        assertEquals("ello world", wrapper.wrap("hello world").tail());
    }


    interface Length {
        int length();
    }

    interface ExtendedString {
        int toInt();
        long toLong();
        boolean toBoolean();
        boolean startsWithIgnoreCase(String other);
        String substring(Integer start, Integer end);
    }

    interface StrangeOperations {
        String tail();
    }
}
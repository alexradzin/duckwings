package org.duckwings;

import org.junit.jupiter.api.Test;

import java.lang.reflect.UndeclaredThrowableException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static java.lang.String.format;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DuckWingsTest {
    @Test
    void reflectiveStringAsCharSequenceLength() {
        assertEquals("hello".length(), DuckWings.builder().reflect(CharSequence.class).wrap("hello").length());
    }

    @Test
    void unwrapReflectiveWrapper() {
        String str = "hello";
        Wrapper<String, CharSequence> wrapper = DuckWings.builder().reflect(CharSequence.class);
        CharSequence wrapped = wrapper.wrap(str);
        assertEquals(str.length(), wrapped.length());
        assertSame(str, wrapper.unwrap(wrapped));
    }

    @Test
    void reflectiveStringAsLength() {
        assertEquals("world".length(), DuckWings.builder().reflect(Length.class).wrap("world").length());
    }

    @Test
    void functionalString() {
        assertEquals("function".length(), DuckWings.builder().functional(Length.class, String.class).using(Length::length, String::length).wrap("function").length());
    }

    @Test
    void unwrapFunctionalWrapper() {
        String str = "hello";
        Wrapper<String, Length> wrapper = DuckWings.builder().functional(Length.class, String.class).using(Length::length, String::length);
        Length wrapped = wrapper.wrap(str);
        assertEquals(str.length(), wrapped.length());
        assertSame(str, wrapper.unwrap(wrapped));
    }

    @Test
    void wrongUnwrap() {
        assertEquals("not a proxy instance", assertThrows(IllegalArgumentException.class, () -> DuckWings.builder().reflect(CharSequence.class).unwrap("hello")).getMessage());
    }

    @Test
    void functionalStringAsCollectionLength() {
        assertEquals("function".length(), DuckWings.builder().functional(Collection.class, String.class).using(Collection::size, String::length).wrap("function").size());
    }

    @Test
    void functionalStringAsCollection() {
        Wrapper<String, Collection> wrapper = DuckWings.builder().functional(Collection.class, String.class)
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

    /**
     * Method {@code length()} does not exist in {@link List} but wrapper ignores this.
     */
    @Test
    void doNotThrowIfMethodDoesNotExist() {
        DuckWings.builder().reflect(Length.class).wrap(new ArrayList<>()).length();
    }


    @Test
    void throwIfAbsentDuringBuildingWhenMethodIsMissing() {
        NoSuchMethodException e = assertThrows(
                NoSuchMethodException.class,
                () -> DuckWings.builder()
                    .throwIfAbsentDuringBuilding((m) -> new NoSuchMethodException(format("Method %s does not exist", m.getName())))
                    .reflect(Length.class).wrap(new ArrayList<>())
        );
        assertEquals("Method length does not exist", e.getMessage());
    }


    @Test
    void throwIfAbsentDuringBuildingWhenMethodExists() {
        assertEquals(0, DuckWings.builder()
                .throwIfAbsentDuringBuilding((m) -> new NoSuchMethodException(format("Method %s does not exist", m.getName())))
                .reflect(Length.class).wrap("").length()); // method length exists in class String
    }

    @Test
    void throwIfAbsentAtRuntimeWhenMethodIsMissing() {
        // Length does not exist in List but this line should not cause exception because the method is validated at runtime only
        Length wrappedLength = DuckWings.builder()
                .throwIfAbsentAtRuntime((m) -> new NoSuchMethodException(format("Method %s does not exist", m.getName())))
                .reflect(Length.class).wrap(new ArrayList<>());

        UndeclaredThrowableException e = assertThrows(UndeclaredThrowableException.class, wrappedLength::length);
        assertEquals("Method length does not exist", e.getCause().getMessage());
    }


    @Test
    void throwIfAbsentAtRuntimeWhenMethodExists() {
        assertEquals("hello".length(),
                DuckWings.builder()
                .throwIfAbsentAtRuntime((m) -> new NoSuchMethodException(format("Method %s does not exist", m.getName())))
                .reflect(Length.class).wrap("hello").length());
    }

    @Test
    void charAtWrongPositionNoException() {
        assertEquals(0, DuckWings.builder().reflect(ExtendedString.class).wrap("hello").charAt(10));
    }

    @Test
    void charAtWrongPositionException() {
        RuntimeException e = assertThrows(RuntimeException.class,
                () -> DuckWings.builder()
                .throwIfAbsentAtRuntime((m) -> new NoSuchMethodException(format("Method %s does not exist", m.getName())))
                .reflect(ExtendedString.class).wrap("hello").charAt(10)
        );
        assertEquals(NoSuchMethodException.class, e.getCause().getClass());
    }


    @Test
    void functionalStringAddFunctions() {
        Wrapper<String, ExtendedString> wrapper = DuckWings.builder().functional(ExtendedString.class, String.class)
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
    void functionalStringAddFunctionsOneIntArg() {
        Wrapper<String, ExtendedString> wrapper = DuckWings.builder().functional(ExtendedString.class, String.class)
                .using(ExtendedString::charAt, String::charAt);

        ExtendedString hello = wrapper.wrap("Hello");
        assertEquals('H', hello.charAt(0));

    }

    @Test
    void functionalStringAddFunctionsTwoArgs() {
        Wrapper<String, ExtendedString> wrapper = DuckWings.builder().functional(ExtendedString.class, String.class)
                .using(ExtendedString::substring, String::substring);
        assertEquals("Hello".substring(2, 3), wrapper.wrap("Hello").substring(2, 3));
    }

    @Test
    void functionalStringAddFunctionsThreeArgs() {
        Wrapper<StringBuilder, ExtendedString> wrapper = DuckWings.builder().functional(ExtendedString.class, StringBuilder.class)
                .using(ExtendedString::replace, StringBuilder::replace);
        assertEquals(new StringBuilder("Hello").replace(2, 4, "LL").toString(), wrapper.wrap(new StringBuilder("Hello")).replace(2, 4, "LL").toString());
    }


    @Test
    void functionalStringAddFunctionsWithDefaultValue() {
        Wrapper<String, StrangeOperations> wrapper = DuckWings.builder().functional(StrangeOperations.class, String.class)
                .using(StrangeOperations::tail, s -> s.substring(s.length() - 10));
        assertEquals("ello world", wrapper.wrap("hello world").tail());
    }

    @Test
    void throwIfAbsentAtRuntimeWhenFunctionExists() {
        // Although method length() does not exit in List exception will not be thrown here. It will be thrown in runtime, i.e. during method invocation
        DuckWings.builder()
                .throwIfAbsentAtRuntime((m) -> new NoSuchMethodException(format("Method %s does not exist", m.getName())))
                .functional(Length.class, List.class).wrap(new ArrayList());


        UndeclaredThrowableException e = assertThrows(
                UndeclaredThrowableException.class,
                () -> DuckWings.builder()
                        .throwIfAbsentAtRuntime((m) -> new NoSuchMethodException(format("Method %s does not exist", m.getName())))
                        .functional(Length.class, List.class).wrap(new ArrayList()).length());
        assertEquals(NoSuchMethodException.class, e.getCause().getClass());
        assertEquals("Method length does not exist", e.getCause().getMessage());
    }

    @Test
    void throwIfAbsentDuringBuildingWhenFunctionExists() {
        NoSuchMethodException e = assertThrows(
                NoSuchMethodException.class,
                () -> DuckWings.builder()
                        .throwIfAbsentDuringBuilding((m) -> new NoSuchMethodException(format("Method %s does not exist", m.getName())))
                        .functional(Length.class, List.class).wrap(new ArrayList()));
        assertEquals("Method length does not exist", e.getMessage());
    }



    interface Length {
        int length();
    }

    interface ExtendedString {
        int toInt();
        long toLong();
        boolean toBoolean();
        boolean startsWithIgnoreCase(String other);
        char charAt(int pos);
        String substring(int start, int end);
        StringBuilder replace(int from, int to, String replacement);
    }

    interface StrangeOperations {
        String tail();
    }
}
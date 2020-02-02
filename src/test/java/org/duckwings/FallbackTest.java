package org.duckwings;

import org.junit.jupiter.api.Test;

import java.util.Calendar;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class FallbackTest {
    @Test
    void functionalWithFallbackToReflectional() {
        Wrapper<Person, PersonalData> wrapper = DuckWings.builder().functional(PersonalData.class, Person.class)
                .using(PersonalData::getFullName, p -> p.getFirstName() + " " + p.getLastName())
                .using(PersonalData::getAge, p -> Calendar.getInstance().get(Calendar.YEAR) - p.getYearOfBirth())
                .fallback(DuckWings.builder().reflect(PersonalData.class));


        Person john = new Person("John", "Lennon", 1940);
        PersonalData w = wrapper.wrap(john);
        assertEquals("John Lennon", w.getFullName());
        assertEquals(Calendar.getInstance().get(Calendar.YEAR) - john.getYearOfBirth(), w.getAge());

        assertEquals("John", w.getFirstName());
        assertEquals("Lennon", w.getLastName());
    }


}

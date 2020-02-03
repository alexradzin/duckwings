package org.duckwings;

import org.junit.jupiter.api.Test;

import java.util.Calendar;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MultipleObjectsTest {
    @Test
    void reflection() {
        Person john = new Person("John", "Lennon", 1940);
        int age = Calendar.getInstance().get(Calendar.YEAR) - 1940;
        NameAndAge lennon = new NameAndAge("John Lennon", age);
        PersonalData w = DuckWings.builder().reflect(PersonalData.class).wrap(john, lennon);

        assertEquals("John Lennon", w.getFullName());
        assertEquals(age, w.getAge());

        assertEquals("John", w.getFirstName());
        assertEquals("Lennon", w.getLastName());
    }


//    @Test
//    void functionalWithFallbackToFunctional() {
//        Wrapper<Person, PersonalData> wrapper = DuckWings.builder().functional(PersonalData.class, Person.class)
//                .using(PersonalData::getFullName, p -> p.getFirstName() + " " + p.getLastName())
//                .using(PersonalData::getAge, p -> Calendar.getInstance().get(Calendar.YEAR) - p.getYearOfBirth())
//                .fallback(DuckWings.builder().functional(PersonalData.class, NameAndAge.class));
//
//
//        DuckWings.builder().functional(PersonalData.class, NameAndAge.class);
//
//
//        Person john = new Person("John", "Lennon", 1940);
//        PersonalData w = wrapper.wrap(john);
//        assertEquals("John Lennon", w.getFullName());
//        assertEquals(Calendar.getInstance().get(Calendar.YEAR) - john.getYearOfBirth(), w.getAge());
//
//        assertEquals("John", w.getFirstName());
//        assertEquals("Lennon", w.getLastName());
//    }



    public class NameAndAge {
        private final String fullName;
        private final int age;

        public NameAndAge(String fullName, int age) {
            this.fullName = fullName;
            this.age = age;
        }

        public String getFullName() {
            return fullName;
        }

        public int getAge() {
            return age;
        }
    }




}

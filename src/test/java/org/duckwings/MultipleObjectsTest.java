package org.duckwings;

import org.junit.jupiter.api.Test;

import java.util.Calendar;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MultipleObjectsTest {
    @Test
    void reflectionalWrapperWithMultipleObjects() {
        Wrapper<Person, PersonalData> wrapper = DuckWings.builder().reflect(PersonalData.class);
        wrapperWithMultipleObjects(wrapper);
    }


    @Test
    void functionalWrapperWithMultipleObjects() {
        Wrapper<Person, PersonalData> wrapper = DuckWings.builder().functional(PersonalData.class, Person.class)
                .using(PersonalData::getFirstName, Person::getFirstName)
                .using(PersonalData::getLastName, Person::getLastName)
                .with(
                        DuckWings.builder().functional(PersonalData.class, NameAndAge.class)
                                .using(PersonalData::getAge, NameAndAge::getAge)
                                .using(PersonalData::getFullName, NameAndAge::getFullName)
                );

        wrapperWithMultipleObjects(wrapper);
    }

    void wrapperWithMultipleObjects(Wrapper<Person, PersonalData> wrapper) {
        Person john = new Person("John", "Lennon", 1940);
        int age = Calendar.getInstance().get(Calendar.YEAR) - 1940;
        NameAndAge lennon = new NameAndAge("John Lennon", age);

        PersonalData w = wrapper.wrap(john, lennon);
        assertEquals("John", w.getFirstName());
        assertEquals("Lennon", w.getLastName());
        assertEquals("John Lennon", w.getFullName());
        assertEquals(Calendar.getInstance().get(Calendar.YEAR) - john.getYearOfBirth(), w.getAge());

    }


    public class NameAndAge {
        private final String fullName;
        private final int age;

        public NameAndAge(String fullName, int age) {
            this.fullName = fullName;
            this.age = age;
        }

        String getFullName() {
            return fullName;
        }

        public int getAge() {
            return age;
        }
    }
}

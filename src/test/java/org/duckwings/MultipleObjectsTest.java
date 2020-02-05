package org.duckwings;

import org.junit.jupiter.api.Test;

import java.util.Calendar;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

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


    @Test
    void reflectionalWrapperMultipleObjectsSecondFailsNoFailureHandler() {
        // The primary object is Integer, secondary is string; we call String's method with wrong argument; this causes exception
        // that is ignored becuase not handler is defined and the default value is returned
        assertEquals(0, DuckWings.builder().reflect(CharSequence.class).wrap(123, "").charAt(-1));
    }

    @Test
    void reflectionalWrapperMultipleObjectsSecondFailsAndThrowsException() {
        assertEquals(
                "charAt",
                assertThrows(
                        IllegalArgumentException.class,
                        () -> DuckWings.builder()
                                .throwIfAbsentAtRuntime(m -> new IllegalArgumentException(m.getName()))
                                .reflect(CharSequence.class)
                                .wrap(123, "")
                                .charAt(-1))
                        .getMessage());
    }

    @Test
    void example() {
        Person person = new Person("John", "Smith", 1970);
        BankAccount account = new BankAccount(123456, "1111-2222-3333-4567");
        Wrapper<Person, PersonalCreditCard> rw = DuckWings.builder().reflect(PersonalCreditCard.class);
        PersonalCreditCard card1 = rw.wrap(person, account);
        assertEquals(account.getCreditCard(), card1.getCreditCard());


        Wrapper<Person, PersonalCreditCard> fw = DuckWings.builder().functional(PersonalCreditCard.class, Person.class)
                .using(PersonalCreditCard::getPersonName, p -> p.getFirstName() + " " + p.getLastName())
                .with(
                    DuckWings.builder().functional(PersonalCreditCard.class, BankAccount.class)
                            .using(PersonalCreditCard::getCreditCard,
                                    a -> IntStream.range(0, 3).boxed().map(i -> Stream.generate(() -> "*").limit(4)
                                            .collect(Collectors.joining())).collect(Collectors.joining("-")) +
                                            "-" +
                                            account.getCreditCard().split("-")[3])
                );


        PersonalCreditCard card2 = fw.wrap(person, account);
        assertEquals("John Smith", card2.getPersonName());
        assertEquals("****-****-****-4567", card2.getCreditCard());

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

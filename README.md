# DuckWings 
[![CircleCI](https://circleci.com/gh/alexradzin/duckwings.svg?style=svg)](https://circleci.com/gh/alexradzin/duckwings)
[![Build Status](https://travis-ci.com/alexradzin/duckwings.svg?branch=master)](https://travis-ci.com/alexradzin/duckwings)
[![codecov](https://codecov.io/gh/alexradzin/duckwings/branch/master/graph/badge.svg)](https://codecov.io/gh/alexradzin/duckwings)
[![Codacy Badge](https://api.codacy.com/project/badge/Grade/bd0a7e8fb5a2469d99d34dcca9a2fd94)](https://www.codacy.com/app/alexradzin/duckwings?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=alexradzin/duckwings&amp;utm_campaign=Badge_Grade)

DuckTyping implementation for Java

## From Wikipedia
Duck typing in computer programming is an application of the duck test — 
"If it walks like a duck and it quacks like a duck, then it must be a duck" — 
to determine if an object can be used for a particular purpose. 
With normal typing, suitability is determined by an object's type. 
In duck typing, an object's suitability is determined by the presence of certain methods and properties, rather than the type of the object itself.

## Motivation
### Poorly designed model
Imagine a bunch of model classes that do not have anything in common in terms of interface but in fact have `id`, `parentId` and `name`. 
All these fields are used to process these object. But there is no way to write general re-usable code that deals with 
all these objects because `id` in class `Book` and in class `Magazine` are different.

One can say that the right solution is just to change the model: define proper interfaces, make the classes to implement them. 
The problem is that this is not always possible: model may be already released or/and a third party code. Moreover in some cases 
we have to work with classes developed by different companies (e.g. `Book` is from company A and `Magazine` from company B). 

### Need to represent something and something else
  * String can be represented as a list of characters.
  * It may be useful to think about repeating call of `Random.nextInt()` as infinite `Iterator<Integer>` that produces random integers.

### Represent information from several objects in unified view
Assume that we have class `Person` that represents the personal data like id, name, date of birth etc. Additionally we have other class named `BankAccount` that holds the bank identifier, the credit card number etc. We can retrieve pairs of `Person` and `BankAccount` and want to expose to objects that hold id, first and last name of the person as well as his/her credit card number. Additionally we have to replace all digits of the credit card except the last 4 digits using `*`. DuckWings helps to solve this problem without boiler plate code. 

## Description
DuckWings is a small library written in java (v >= 8) that provides 2 implementations of Duck typing programming paradigm:
  * reflection based
  * functional

DuckWings helps to wrap any object by interface originally not implemented by the class and then use the class as it implements the required interface. DuckWings uses dynamic proxy under the hood. 

## Alternatives

### Manually created adaptor class
This solution is not brain intensive but requires to write a lot of boiler plate code

### Creating alternative model
This solution requires even more boiler plate code and requires serious maintenance efforts. You have to implement the new model
itself as well as convertors from old to new classes and back. The a log of maintenance is required if the original model is changed.
All solutions that include copying data from one model to another in some cases have serious performance penalty as well. 
Improved version of this solution may avoid coding of inter model converters by using frameworks like [Dozer](https://github.com/DozerMapper/dozer).

## Simple usage

```java
// Model
class Book {
    int id;
    String isbn;
    // getters & setters
}

class Magazine {
    int id;
    String isbn;
    // getters & setters
}


// Interface (not implemented by model)
interface IdHolder {
    int getId();
    String getIsbn();
}

// Now we can use these classes as following
Wrapper<Object, IdHolder> wrapper = DuckWings.builder().reflect(IdHolder.class);
IdHolder book = wrapper.wrap(new Book());
IdHolder magazine = wrapper.wrap(new Magazine());
List<IdHolder> list = Arrays.asList(book, magazine);
```

The prevous example showed usage of reflection based implementation. Other, functional implementation requires proprietery mapping
of functions from interface to thier mirrors in class but is more flexible and shows better performance.

```java
Wrapper<Book, IdHolder> bookWrapper = JDuck.builder().functional(IdHolder.class, Book.class)
        .using(IdHolder::getId, Book::getId)
        .using(IdHolder::getIsbn, Book::getIsbn);

Wrapper<Magazine, IdHolder> magazineWrapper = JDuck.builder().functional(IdHolder.class, Magazine.class)
        .using(IdHolder::getId, Magazine::getId)
        .using(IdHolder::getIsbn, Magazine::getIsbn);

IdHolder book = bookWrapper.wrap(new Book());
IdHolder magazine = magazineWrapper.wrap(new Magazine());
List<IdHolder> list = Arrays.asList(book, magazine);
```

In examples explained above both classes `Book` and `Magazine` had methods with the same name and signature. JDuck helps even this is not the case. Class `String` has method `length()`, `Collection` declares method `size()`. 

```java
interface Length {
    int length();
}
Wrapper<String, Length> strWrapper = JDuck.builder().functional(Length.class, String.class)
        .using(Length::length, String::length);
Wrapper<Collection, Length> colWrapper = JDuck.builder().functional(Length.class, Collection.class)
        .using(Length::length, Collection::size);
```
Now we can wrap string and collection and access their length using uniform way. 


## Advanced features
### Fallback
We have class `Person`:

```java
class Person {
    int id;
    String firstName;
    String lastName;
    // getters & setters
}
```
and want to expose its data as it `Person` was implementing interface `PersonalData`:
```java
interface PersonalData {
    String getFirstName();
    String getLastName();
    String getFullName(); // concatenation of first and last name with space between them
}
```
Methods `getFirstName()` and `getLastName()` can be mapped directly to the coresponding implementation of class `Person` but method `getFullName()` must be implemented. We can do this as following using functional builder:
```java
        Wrapper<Person, PersonalData> wrapper = DuckWings.builder().functional(PersonalData.class, Person.class)
                .using(PersonalData::getFirstName, Person::getFirstName)
                .using(PersonalData::getLastName, Person::getLastName)
                .using(PersonalData::getFullName, p -> p.getFirstName() + " " + p.getLastName());
```
However we want to avoid boiler plate code that maps `getFirstName` and `getLastName`. This can be achieved using fallback to reflectional implementation:
```java
        Wrapper<Person, PersonalData> wrapper = DuckWings.builder().functional(PersonalData.class, Person.class)
                .using(PersonalData::getFullName, p -> p.getFirstName() + " " + p.getLastName())
                .fallback(DuckWings.builder().reflect(PersonalData.class));
```

### View over multiple objects
TBD (implemented, not described yet)

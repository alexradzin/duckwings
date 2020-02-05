package org.duckwings;

public class BankAccount {
    private final int id;
    private final String creditCard;

    public BankAccount(int id, String creditCard) {
        this.id = id;
        this.creditCard = creditCard;
    }

    public int getId() {
        return id;
    }

    public String getCreditCard() {
        return creditCard;
    }
}

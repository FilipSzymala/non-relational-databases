package pl.nbd.bank.bank.client;

public record Address(String country, String city, String street, String streetNumber, String apartmentNumber, String zipCode) {
    public Address {
    }
}
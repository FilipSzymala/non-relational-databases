package pl.nbd.bank.bank.client;

public record Address(String country, String city, String street, String streetNumber, String apartmentNumber, String zipCode) {
    public Address {
        if (
            country.isBlank() || city.isBlank() || street.isBlank() || streetNumber.isBlank() || zipCode.isBlank()
        ) {
            throw new IllegalArgumentException(
                "All address fields (excluding apartmentNumber) must not be blank (country, city, street, streetNumber, zipCode)"
            );
        }
    }
}
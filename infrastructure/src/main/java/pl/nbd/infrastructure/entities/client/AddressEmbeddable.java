package pl.nbd.infrastructure.entities.client;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class AddressEmbeddable {
    @Column (name = "country", nullable = false)
    private String country;
    @Column (name = "city", nullable = false)
    private String city;
    @Column (name = "street", nullable = false)
    private String street;
    @Column (name = "street_number", nullable = false)
    private String streetNumber;
    @Column (name = "apartment_number", nullable = true)
    private String apartmentNumber;
    @Column (name = "zip_code", nullable = false)
    private String zipCode;

    protected AddressEmbeddable() {
    }

    public AddressEmbeddable(String country, String city, String street, String streetNumber, String zipCode) {
        this.country = country;
        this.city = city;
        this.street = street;
        this.streetNumber = streetNumber;
        this.zipCode = zipCode;
    }

    public AddressEmbeddable(String country, String city, String street, String streetNumber, String apartmentNumber, String zipCode) {
        this.country = country;
        this.city = city;
        this.street = street;
        this.streetNumber = streetNumber;
        this.apartmentNumber = apartmentNumber;
        this.zipCode = zipCode;
    }

    public String getCountry() {
        return country;
    }

    public String getCity() {
        return city;
    }

    public String getStreet() {
        return street;
    }

    public String getStreetNumber() {
        return streetNumber;
    }

    public String getApartmentNumber() {
        return apartmentNumber;
    }

    public String getZipCode() {
        return zipCode;
    }
}

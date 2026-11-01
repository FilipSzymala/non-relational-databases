package pl.nbd.infrastructure.documents.client;

import org.bson.codecs.pojo.annotations.BsonCreator;
import org.bson.codecs.pojo.annotations.BsonProperty;

public class AddressMgd {
    private String country;
    private String city;
    private String street;
    @BsonProperty("street_number")
    private String streetNumber;
    @BsonProperty("apartment_number")
    private String apartmentNumber;
    @BsonProperty("zip_code")
    private String zipCode;

    @BsonCreator
    public AddressMgd(
        @BsonProperty("country") String country,
        @BsonProperty("city") String city,
        @BsonProperty("street") String street,
        @BsonProperty("street_number") String streetNumber,
        @BsonProperty("apartment_number") String apartmentNumber,
        @BsonProperty("zip_code") String zipCode
                     ) {
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

package pl.nbd.infrastructure.documents.client;

import org.bson.codecs.pojo.annotations.BsonCreator;
import org.bson.codecs.pojo.annotations.BsonId;
import org.bson.codecs.pojo.annotations.BsonProperty;

import java.util.UUID;

public class ClientEmbeddedMgd {
    @BsonId
    private final UUID id;
    @BsonProperty("first_name")
    private final String firstName;
    @BsonProperty("last_name")
    private final String lastName;
    @BsonProperty("personal_id")
    private final PersonalIdMgd personalId;
    private final AddressMgd address;

    @BsonCreator
    public ClientEmbeddedMgd(@BsonId UUID id, @BsonProperty("first_name") String firstName, @BsonProperty("last_name") String lastName, @BsonProperty("personal_id") PersonalIdMgd personalId, @BsonProperty("address") AddressMgd address) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.personalId = personalId;
        this.address = address;
    }

    public UUID getId() {
        return id;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public PersonalIdMgd getPersonalId() {
        return personalId;
    }

    public AddressMgd getAddress() {
        return address;
    }
}

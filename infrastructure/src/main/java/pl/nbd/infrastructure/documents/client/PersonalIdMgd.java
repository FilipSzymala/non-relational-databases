package pl.nbd.infrastructure.documents.client;

import org.bson.codecs.pojo.annotations.BsonCreator;

import org.bson.codecs.pojo.annotations.BsonProperty;

public class PersonalIdMgd {
    private final String value;

    @BsonCreator
    public PersonalIdMgd(@BsonProperty("value") String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}

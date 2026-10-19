package pl.nbd.infrastructure.entities.client;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class PersonalIdEmbeddable {
    @Column(name = "personal_id", nullable = false, length = 11)
    private String personalId;

    protected PersonalIdEmbeddable() {
    }

    public PersonalIdEmbeddable(String personalId) {
        this.personalId = personalId;
    }

    public String getPersonalId() {
        return personalId;
    }
}

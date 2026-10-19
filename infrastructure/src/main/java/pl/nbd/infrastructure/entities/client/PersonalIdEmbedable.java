package pl.nbd.infrastructure.entities.client;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class PersonalIdEmbedable {
    @Column(name = "personal_id", nullable = false, length = 11)
    private String personalId;

    protected PersonalIdEmbedable() {
    }

    public PersonalIdEmbedable(String personalId) {
        this.personalId = personalId;
    }

    public String getPersonalId() {
        return personalId;
    }
}

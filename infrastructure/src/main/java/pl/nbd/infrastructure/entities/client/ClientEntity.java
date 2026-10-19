package pl.nbd.infrastructure.entities.client;

import jakarta.persistence.*;

import java.util.UUID;

@Entity
@Table(name = "clients")
public class ClientEntity {
    @Id
    private UUID id;
    @Column(name = "first_name", nullable = false)
    private String firstName;
    @Column(name = "last_name", nullable = false)
    private String lastName;
    @Embedded
    private PersonalIdEmbedable personalId;
    @Embedded
    private AddressEmbeddable address;

    protected ClientEntity() {
    }

    public ClientEntity(UUID id, String firstName, String lastName, PersonalIdEmbedable personalId, AddressEmbeddable address) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.personalId = personalId;
        this.address = address;
    }

}

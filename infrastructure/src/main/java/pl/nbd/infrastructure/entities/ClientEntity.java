package pl.nbd.infrastructure.entities;

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

    protected ClientEntity() {
    }

    public ClientEntity(UUID id, String firstName, String lastName) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
    }
}

package pl.nbd.infrastructure.fixtures;

import pl.nbd.infrastructure.entities.client.AddressEmbeddable;
import pl.nbd.infrastructure.entities.client.ClientEntity;
import pl.nbd.infrastructure.entities.client.PersonalIdEmbedable;

import java.util.UUID;

public final class ClientEntityFactory {
    private ClientEntityFactory() {
    }

    public static ClientEntity createClient() {
        return createClient(UUID.randomUUID());
    }

    public static ClientEntity createClient(UUID id) {
        PersonalIdEmbedable personalId = new PersonalIdEmbedable("12345678901");
        AddressEmbeddable address = new AddressEmbeddable(
            "Polska",
            "Płock",
            "Komendancka",
            "60",
            "12",
            "12-321"
        );
        return new ClientEntity(id, "Kamil", "Kowalenko", personalId, address);
    }
}

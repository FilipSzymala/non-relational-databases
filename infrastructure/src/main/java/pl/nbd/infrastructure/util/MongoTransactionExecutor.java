package pl.nbd.infrastructure.util;

import com.mongodb.ReadConcern;
import com.mongodb.ReadPreference;
import com.mongodb.TransactionOptions;
import com.mongodb.WriteConcern;
import com.mongodb.client.ClientSession;
import com.mongodb.client.MongoClient;
import pl.nbd.bank.repository.AccountRepository;
import pl.nbd.bank.repository.ClientRepository;
import pl.nbd.bank.repository.OperationRepository;
import pl.nbd.infrastructure.adapters.MongoAccountRepositoryAdapter;
import pl.nbd.infrastructure.adapters.MongoClientRepositoryAdapter;
import pl.nbd.infrastructure.adapters.MongoOperationRepositoryAdapter;
import pl.nbd.infrastructure.mappers.AccountMapper;
import pl.nbd.infrastructure.mappers.ClientMapper;
import pl.nbd.infrastructure.mappers.OperationMapper;
import pl.nbd.infrastructure.repositories.MongoAccountRepository;
import pl.nbd.infrastructure.repositories.MongoClientRepository;
import pl.nbd.infrastructure.repositories.MongoOperationRepository;

import java.util.Objects;
import java.util.function.Function;

public class MongoTransactionExecutor {

    private final MongoClient mongoClient;

    private final TransactionOptions options = TransactionOptions.builder()
        .readConcern(ReadConcern.SNAPSHOT)
        .writeConcern(WriteConcern.MAJORITY)
        .readPreference(ReadPreference.primary())
        .build();

    public MongoTransactionExecutor(MongoClient mongoClient) {
        this.mongoClient = Objects.requireNonNull(mongoClient);
    }

    public <T> T execute(Function<Repositories, T> action) {
        Objects.requireNonNull(action, "Transaction action must not be null");

        try (ClientSession session = mongoClient.startSession()) {
            return session.withTransaction(() -> {
                MongoClientRepository clients = new MongoClientRepository(mongoClient, session);
                MongoAccountRepository accounts = new MongoAccountRepository(mongoClient, session);
                MongoOperationRepository operations = new MongoOperationRepository(mongoClient, session);

                AccountMapper accountMapper = new AccountMapper();

                return action.apply(new Repositories(
                    new MongoClientRepositoryAdapter(clients, accounts, new ClientMapper(), accountMapper),
                    new MongoAccountRepositoryAdapter(accounts, accountMapper),
                    new MongoOperationRepositoryAdapter(operations, new OperationMapper())
                ));
            }, options);
        }
    }

    public record Repositories(
        ClientRepository clients,
        AccountRepository accounts,
        OperationRepository operations
    ) {
    }
}

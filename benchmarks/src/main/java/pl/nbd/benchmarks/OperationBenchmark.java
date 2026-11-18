package pl.nbd.benchmarks;

import com.mongodb.client.MongoClient;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import org.openjdk.jmh.annotations.*;
import pl.nbd.bank.bank.operation.AtmDeposit;
import pl.nbd.bank.bank.operation.Operation;
import pl.nbd.bank.bank.shared.Money;
import pl.nbd.bank.repository.OperationRepository;
import pl.nbd.infrastructure.adapters.MongoOperationRepositoryAdapter;
import pl.nbd.infrastructure.adapters.RedisOperationRepositoryAdapter;
import pl.nbd.infrastructure.mappers.OperationMapper;
import pl.nbd.infrastructure.repositories.MongoOperationRepository;
import pl.nbd.infrastructure.repositories.RedisOperationRepository;
import pl.nbd.infrastructure.util.RedisClientFactory;
import redis.clients.jedis.JedisPooled;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static com.mongodb.client.model.Filters.eq;

@State(Scope.Thread)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(2)
@Threads(1)
public class OperationBenchmark {
    @Param({"mongo", "hit", "miss"})
    public String source;
    private MongoClient mongoClient;
    private JedisPooled redis;
    private Jsonb jsonb;
    private RedisOperationRepository cache;
    private CountingRepository mongo;
    private RedisOperationRepositoryAdapter cached;
    private UUID id;
    private long invocations;

    @Setup(Level.Trial)
    public void open() {
        mongoClient = BenchmarkDatabase.open();
        redis = RedisClientFactory.create();
        redis.ping();
        jsonb = JsonbBuilder.create();
        cache = new RedisOperationRepository(redis, RedisClientFactory.ttlSeconds(), jsonb);
        OperationMapper mapper = new OperationMapper();
        mongo = new CountingRepository(new MongoOperationRepositoryAdapter(
            new MongoOperationRepository(mongoClient), mapper));
        cached = new RedisOperationRepositoryAdapter(mongo, cache, mapper);
        Operation operation = new AtmDeposit(Money.pln("123.45"), UUID.randomUUID(), "BENCHMARK");
        id = operation.getId();
        cached.save(operation);
        cached.findById(id).orElseThrow();
        if (cache.findById(id).isEmpty()) {
            throw new IllegalStateException("Cache was not populated");
        }
    }

    @Setup(Level.Iteration)
    public void prepareIteration() {
        redis.ping();
        if (source.equals("hit") && cache.findById(id).isEmpty()) {
            throw new IllegalStateException("Hit benchmark requires a cached value");
        }
        mongo.reads = 0;
        invocations = 0;
    }

    @Setup(Level.Invocation)
    public void prepareInvocation() {
        if (source.equals("miss")) {
            cached.invalidate(id);
            if (cache.findById(id).isPresent()) {
                throw new IllegalStateException("Cache invalidation failed");
            }
        }
    }

    @Benchmark
    public Operation read() {
        invocations++;
        return (source.equals("mongo") ? mongo : cached).findById(id).orElseThrow();
    }

    @TearDown(Level.Invocation)
    public void verifyCacheFill() {
        if (source.equals("miss") && cache.findById(id).isEmpty()) {
            throw new IllegalStateException("Miss did not populate the cache");
        }
    }

    @TearDown(Level.Iteration)
    public void verifyIteration() {
        long expectedMongoReads = source.equals("hit") ? 0 : invocations;
        if (mongo.reads != expectedMongoReads) {
            throw new IllegalStateException("Unexpected MongoDB read count: " + mongo.reads
                + "; expected " + expectedMongoReads);
        }
        if (!source.equals("mongo") && cache.findById(id).isEmpty()) {
            throw new IllegalStateException("Cache was not available after the iteration");
        }
        redis.ping();
    }

    @TearDown(Level.Trial)
    public void close() throws Exception {
        try {
            if (cache != null && id != null) {
                cache.deleteById(id);
            }
        } finally {
            try {
                if (mongoClient != null && id != null) {
                    mongoClient.getDatabase("bank").getCollection("operations").deleteOne(eq("_id", id));
                }
            } finally {
                try {
                    if (jsonb != null) {
                        jsonb.close();
                    }
                } finally {
                    try {
                        if (redis != null) {
                            redis.close();
                        }
                    } finally {
                        if (mongoClient != null) {
                            mongoClient.close();
                        }
                    }
                }
            }
        }
    }

    private static final class CountingRepository implements OperationRepository {
        private final OperationRepository delegate;
        private long reads;

        private CountingRepository(OperationRepository delegate) {
            this.delegate = delegate;
        }

        @Override
        public Optional<Operation> findById(UUID id) {
            reads++;
            return delegate.findById(id);
        }

        @Override
        public List<Operation> findByAccountId(UUID accountId) {
            return delegate.findByAccountId(accountId);
        }

        @Override
        public void save(Operation operation) {
            delegate.save(operation);
        }
    }
}

package team.carrypigeon.backend.api.connection.pool;

@FunctionalInterface
public interface ConnectionPoolStarter {
    void run(int port);
}
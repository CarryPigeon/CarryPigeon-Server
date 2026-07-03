package team.carrypigeon.backend.infrastructure.service.database.impl.env;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import java.time.Instant;
import javax.sql.DataSource;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import team.carrypigeon.backend.infrastructure.service.database.api.auth.account.AuthAccountRecord;
import team.carrypigeon.backend.infrastructure.service.database.api.auth.session.AuthRefreshSessionRecord;
import team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.auth.account.AuthAccountMapper;
import team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.auth.account.MybatisPlusAuthAccountDatabaseService;
import team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.auth.session.AuthRefreshSessionMapper;
import team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.auth.session.MybatisPlusAuthRefreshSessionDatabaseService;
import team.carrypigeon.backend.infrastructure.service.database.impl.testsupport.EnvTestDataScope;

/**
 * Real MySQL environment tests for auth database services.
 * Contract: database-impl can persist, query, update, revoke, and clean test-owned auth rows against the real schema.
 * Boundary: this test runs only when explicitly enabled and never falls back to mock mappers.
 */
@Tag("env")
@Tag("env-db")
class AuthDatabaseServiceEnvTests {

    private static final Instant CREATED_AT = Instant.parse("2026-07-03T04:56:53Z");
    private static final Instant UPDATED_AT = Instant.parse("2026-07-03T04:57:53Z");

    /**
     * Verifies the real auth account mapper and refresh-session mapper against a prepared MySQL schema.
     */
    @Test
    @DisplayName("real mysql auth account and refresh session round trip")
    void authServices_realMysql_roundTripAndCleanup() throws Exception {
        EnvDatabaseSettings settings = EnvDatabaseSettings.fromEnvironment();
        assumeTrue(settings.enabled(), "set CP_ENV_DB_TEST_ENABLED=true or -Dcp.env.db.test.enabled=true to run env-db tests");

        DataSource dataSource = settings.dataSource();
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        assertRequiredTablesExist(jdbcTemplate);

        SqlSessionFactory sqlSessionFactory = sqlSessionFactory(dataSource);
        String username;
        try (SqlSession sqlSession = sqlSessionFactory.openSession(false);
             EnvTestDataScope dataScope = EnvTestDataScope.create("auth-db-round-trip")) {
            AuthAccountMapper accountMapper = sqlSession.getMapper(AuthAccountMapper.class);
            AuthRefreshSessionMapper sessionMapper = sqlSession.getMapper(AuthRefreshSessionMapper.class);
            MybatisPlusAuthAccountDatabaseService accountService = new MybatisPlusAuthAccountDatabaseService(accountMapper);
            MybatisPlusAuthRefreshSessionDatabaseService sessionService = new MybatisPlusAuthRefreshSessionDatabaseService(sessionMapper);

            long accountId = positiveId(dataScope.namespace().hashCode());
            long sessionId = accountId + 1;
            username = dataScope.key("account");
            String refreshTokenHash = sha256LikeHash(dataScope.key("refresh"));

            dataScope.cleanup("auth_account " + accountId, () -> jdbcTemplate.update(
                    "DELETE FROM auth_account WHERE id = ?", accountId));
            dataScope.cleanup("auth_refresh_session " + sessionId, () -> jdbcTemplate.update(
                    "DELETE FROM auth_refresh_session WHERE id = ?", sessionId));

            accountService.insert(new AuthAccountRecord(accountId, username, "hash-before", CREATED_AT, UPDATED_AT));
            sessionService.insert(new AuthRefreshSessionRecord(
                    sessionId,
                    accountId,
                    refreshTokenHash,
                    CREATED_AT.plusSeconds(3600),
                    false,
                    CREATED_AT,
                    UPDATED_AT
            ));
            sqlSession.commit();

            AuthAccountRecord foundByUsername = accountService.findByUsername(username).orElseThrow();
            assertEquals(accountId, foundByUsername.id());
            assertEquals("hash-before", foundByUsername.passwordHash());

            accountService.update(new AuthAccountRecord(accountId, username, "hash-after", CREATED_AT, UPDATED_AT.plusSeconds(60)));
            sqlSession.commit();
            AuthAccountRecord foundById = accountService.findById(accountId).orElseThrow();
            assertEquals("hash-after", foundById.passwordHash());

            AuthRefreshSessionRecord sessionBeforeRevoke = sessionService.findById(sessionId).orElseThrow();
            assertFalse(sessionBeforeRevoke.revoked());

            sessionService.revoke(sessionId);
            sqlSession.commit();
            AuthRefreshSessionRecord sessionAfterRevoke = sessionService.findById(sessionId).orElseThrow();
            assertTrue(sessionAfterRevoke.revoked());
        }

        assertEquals(0, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM auth_account WHERE username = ?",
                Integer.class,
                username
        ));
    }

    private static SqlSessionFactory sqlSessionFactory(DataSource dataSource) throws Exception {
        MybatisSqlSessionFactoryBean factoryBean = new MybatisSqlSessionFactoryBean();
        factoryBean.setDataSource(dataSource);
        factoryBean.setTypeAliasesPackage("team.carrypigeon.backend.infrastructure.service.database.impl.mybatis");
        MybatisConfiguration configuration = new MybatisConfiguration();
        configuration.addMapper(AuthAccountMapper.class);
        configuration.addMapper(AuthRefreshSessionMapper.class);
        factoryBean.setConfiguration(configuration);
        return factoryBean.getObject();
    }

    private static void assertRequiredTablesExist(JdbcTemplate jdbcTemplate) {
        Integer accountTableCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'auth_account'",
                Integer.class
        );
        Integer sessionTableCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'auth_refresh_session'",
                Integer.class
        );
        assumeTrue(accountTableCount != null && accountTableCount == 1, "auth_account table is missing; run docs/sql/00-all-in-one.sql first");
        assumeTrue(sessionTableCount != null && sessionTableCount == 1, "auth_refresh_session table is missing; run docs/sql/00-all-in-one.sql first");
    }

    private static long positiveId(int hash) {
        long positive = Integer.toUnsignedLong(hash);
        return 9_000_000_000_000L + positive;
    }

    private static String sha256LikeHash(String source) {
        String normalized = source.replace("_", "");
        return (normalized + "0000000000000000000000000000000000000000000000000000000000000000")
                .substring(0, 64);
    }

    private record EnvDatabaseSettings(boolean enabled, String url, String username, String password) {

        static EnvDatabaseSettings fromEnvironment() {
            boolean enabled = booleanValue("cp.env.db.test.enabled", "CP_ENV_DB_TEST_ENABLED");
            String host = value("cp.env.db.host", "CP_ENV_DB_HOST", "127.0.0.1");
            String port = value("cp.env.db.port", "CP_ENV_DB_PORT", "3306");
            String database = value("cp.env.db.database", "CP_ENV_DB_DATABASE", "carrypigeon");
            String username = value("cp.env.db.username", "CP_ENV_DB_USERNAME", "carrypigeon");
            String password = value("cp.env.db.password", "CP_ENV_DB_PASSWORD", "carrypigeon123");
            String url = value(
                    "cp.env.db.url",
                    "CP_ENV_DB_URL",
                    "jdbc:mysql://" + host + ":" + port + "/" + database
                            + "?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai"
            );
            return new EnvDatabaseSettings(enabled, url, username, password);
        }

        DataSource dataSource() {
            DriverManagerDataSource dataSource = new DriverManagerDataSource();
            dataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");
            dataSource.setUrl(url);
            dataSource.setUsername(username);
            dataSource.setPassword(password);
            return dataSource;
        }

        private static boolean booleanValue(String propertyName, String envName) {
            return Boolean.parseBoolean(value(propertyName, envName, "false"));
        }

        private static String value(String propertyName, String envName, String defaultValue) {
            String propertyValue = System.getProperty(propertyName);
            if (propertyValue != null && !propertyValue.isBlank()) {
                return propertyValue;
            }
            String envValue = System.getenv(envName);
            if (envValue != null && !envValue.isBlank()) {
                return envValue;
            }
            return defaultValue;
        }
    }
}

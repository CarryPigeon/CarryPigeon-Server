package team.carrypigeon.backend.dao.database;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class CPDaoDatabaseConfigurationTests {

    @Test
    void init_shouldNotThrow() {
        CPDaoDatabaseConfiguration config = new CPDaoDatabaseConfiguration();
        assertDoesNotThrow(config::init);
    }
}


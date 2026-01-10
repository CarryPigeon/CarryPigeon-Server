package team.carrypigeon.backend.starter;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
        "cp.startup.health-check.enabled=false",
        "connection.enabled=false"
})
class ApplicationstarterApplicationTests {

    @Test
    void contextLoads() {
    }

}

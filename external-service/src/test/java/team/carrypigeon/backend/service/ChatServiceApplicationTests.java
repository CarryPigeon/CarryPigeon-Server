package team.carrypigeon.backend.service;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.mail.javamail.JavaMailSender;

@SpringBootTest(classes = ChatServiceApplicationTests.TestApplication.class)
class ChatServiceApplicationTests {

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @ComponentScan(basePackages = "team.carrypigeon.backend.chat.service.email")
    static class TestApplication {
        @Bean
        JavaMailSender javaMailSender() {
            return Mockito.mock(JavaMailSender.class);
        }
    }

    @Test
    void contextLoads() {
    }

}

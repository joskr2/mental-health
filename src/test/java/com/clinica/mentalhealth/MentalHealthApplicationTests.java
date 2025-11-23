package com.clinica.mentalhealth;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
        "spring.ai.openai.api-key=dummy",
        "spring.ai.openai.base-url=https://api.deepseek.com"
})
class MentalHealthApplicationTests {

    @Test
    void contextLoads() {
    }

}
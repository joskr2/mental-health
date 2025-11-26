package com.clinica.mentalhealth;

import org.junit.jupiter.api.Test;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertTrue;

@ActiveProfiles("test")
class MentalHealthApplicationTests {

    @Test
    void contextLoads() {
        // Si llegamos aquí sin excepción, el contexto cargó correctamente
        assertTrue(true);
    }

}

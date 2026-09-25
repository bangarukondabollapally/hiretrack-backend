package com.hiretrack;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * TASK-001 smoke test — verifies the Spring application context loads
 * without errors (DataSource excluded, no DB required).
 */
@SpringBootTest
class HiretrackApplicationTests {

    @Test
    void contextLoads() {
        // If the context fails to load, this test fails automatically.
        // No assertions needed — context startup is the assertion.
    }
}

package com.charitymanagement.api.charitymanagementback;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/** Proves the whole bean graph — every module, filter and configuration class — wires up. */
@SpringBootTest
@ActiveProfiles("test")
class CharityManagementBackApplicationTests {

    @Test
    void contextLoads() {
    }

}

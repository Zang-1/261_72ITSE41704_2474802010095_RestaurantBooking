package vn.edu.vlu.restaurant.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.time.ZoneId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * Base class for API tests: full application on an in-memory H2 database with the seeded accounts
 * and tables (DataSeeder). Each test runs in a transaction that is rolled back afterwards.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
abstract class ApiTestSupport {

    protected static final ZoneId RESTAURANT_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    protected String json(Object body) throws JsonProcessingException {
        return objectMapper.writeValueAsString(body);
    }

    protected LocalDate tomorrow() {
        return LocalDate.now(RESTAURANT_ZONE).plusDays(1);
    }
}

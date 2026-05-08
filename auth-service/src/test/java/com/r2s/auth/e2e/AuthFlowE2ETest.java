package com.r2s.auth.e2e;

import com.r2s.auth.dto.LoginRequest;
import com.r2s.auth.dto.RegisterRequest;
import com.r2s.auth.entity.Role;
import com.r2s.auth.entity.User;
import com.r2s.auth.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@EmbeddedKafka(partitions = 1, topics = {"user-registered-topic", "user-deleted-topic"})
@DisplayName("E2E Test: Authentication Flow")
class AuthFlowE2ETest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String baseUrl;

    @BeforeEach
    void setUp() {
        baseUrl = "http://localhost:" + port;
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("TC057 - E2E: Register -> Login -> Token hop le")
    void e2e_RegisterThenLogin_ReturnsValidToken() {
        // Step 1: Register
        RegisterRequest registerReq = new RegisterRequest();
        registerReq.setUsername("e2euser");
        registerReq.setPassword("password123");

        ResponseEntity<String> registerResponse = restTemplate.postForEntity(
                baseUrl + "/auth/register",
                registerReq,
                String.class
        );

        assertEquals(HttpStatus.OK, registerResponse.getStatusCode());

        // Step 2: Verify user da co trong DB
        User savedUser = userRepository.findByUsername("e2euser").orElseThrow();
        assertEquals("e2euser", savedUser.getUsername());
        assertEquals(Role.ROLE_USER, savedUser.getRole());

        // Step 3: Login
        LoginRequest loginReq = new LoginRequest();
        loginReq.setUsername("e2euser");
        loginReq.setPassword("password123");

        ResponseEntity<String> loginResponse = restTemplate.postForEntity(
                baseUrl + "/auth/login",
                loginReq,
                String.class
        );

        // Step 4: Verify login thanh cong va tra token
        assertEquals(HttpStatus.OK, loginResponse.getStatusCode());
        assertNotNull(loginResponse.getBody());
        assertTrue(loginResponse.getBody().contains("token"));
    }

    @Test
    @DisplayName("TC058 - E2E: Admin assign role -> User co quyen truy cap")
    void e2e_AssignRole_AccessChanges() {
        // Step 1: Tao admin truc tiep trong DB
        User admin = new User();
        admin.setUsername("admin_e2e");
        admin.setPassword(passwordEncoder.encode("admin123"));
        admin.setRole(Role.ROLE_ADMIN);
        userRepository.save(admin);

        // Step 2: Tao user thuong
        User user = new User();
        user.setUsername("regular_user");
        user.setPassword(passwordEncoder.encode("user123"));
        user.setRole(Role.ROLE_USER);
        userRepository.save(user);

        // Step 3: Admin login lay token
        LoginRequest adminLogin = new LoginRequest();
        adminLogin.setUsername("admin_e2e");
        adminLogin.setPassword("admin123");

        ResponseEntity<String> adminLoginRes = restTemplate.postForEntity(
                baseUrl + "/auth/login",
                adminLogin,
                String.class
        );
        assertEquals(HttpStatus.OK, adminLoginRes.getStatusCode());
        String adminToken = extractToken(adminLoginRes.getBody());

        // Step 4: Admin assign role MODERATOR cho user
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(adminToken);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        String url = baseUrl + "/auth/admin/assign-role"
                + "?username=regular_user&role=ROLE_MODERATOR";

        ResponseEntity<String> assignRes = restTemplate.exchange(
                url,
                HttpMethod.POST,
                entity,
                String.class
        );

        // Verify response 200 OK
        assertEquals(HttpStatus.OK, assignRes.getStatusCode(),
                "Assign role phải thành công, body: " + assignRes.getBody());

        // Step 5: Verify role thay doi trong DB
        User updatedUser = userRepository.findByUsername("regular_user").orElseThrow();
        assertEquals(Role.ROLE_MODERATOR, updatedUser.getRole());
    }

    private String extractToken(String json) {
        int idx = json.indexOf("\"token\":");
        if (idx < 0) return null;
        int start = json.indexOf("\"", idx + 8) + 1;
        int end = json.indexOf("\"", start);
        return json.substring(start, end);
    }
}
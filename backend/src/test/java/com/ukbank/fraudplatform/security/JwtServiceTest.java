package com.ukbank.fraudplatform.security;

import com.ukbank.fraudplatform.model.Role;
import com.ukbank.fraudplatform.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "jwtSecret", "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970");
        ReflectionTestUtils.setField(jwtService, "jwtExpirationMs", 3600000L); // 1 hour
    }

    @Test
    @DisplayName("Generated token should be valid and extract subject username and role correctly")
    void testTokenGenerationAndValidation() {
        User user = User.builder()
                .id(UUID.randomUUID())
                .username("sarah.analyst")
                .passwordHash("hashed")
                .email("sarah@ukbank.co.uk")
                .fullName("Sarah Analyst")
                .role(Role.ROLE_FRAUD_ANALYST)
                .build();

        CustomUserDetails userDetails = new CustomUserDetails(user);
        String token = jwtService.generateToken(userDetails, user.getRole().name());

        assertNotNull(token);
        assertTrue(token.length() > 20);

        String extractedUsername = jwtService.extractUsername(token);
        assertEquals("sarah.analyst", extractedUsername);

        String extractedRole = jwtService.extractRole(token);
        assertEquals("ROLE_FRAUD_ANALYST", extractedRole);

        assertTrue(jwtService.isTokenValid(token, userDetails));
    }
}

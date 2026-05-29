package com.r2s.auth.testdata;

import com.r2s.auth.dto.LoginRequest;
import com.r2s.auth.dto.RegisterRequest;
import com.r2s.auth.entity.Role;
import com.r2s.auth.entity.User;

/**
 * Builder Pattern de tao test data (DRY + Clean Code).
 *
 * <p>Tap trung logic tao test data vao 1 cho. Khi schema entity doi,
 * chi can sua o day - tat ca test tu cap nhat.
 */
public final class TestDataBuilder {

    public static final String TEST_USERNAME = "newuser";
    public static final String TEST_PASSWORD = "123456";
    public static final String TEST_ENCODED_PASSWORD = "encodedPassword";
    public static final String TEST_TOKEN = "mockedToken";
    public static final String ADMIN_USERNAME = "admin";

    private TestDataBuilder() {}  // Utility class - khong cho new

    /** Tao RegisterRequest mac dinh. */
    public static RegisterRequest aRegisterRequest() {
        RegisterRequest r = new RegisterRequest();
        r.setUsername(TEST_USERNAME);
        r.setPassword(TEST_PASSWORD);
        return r;
    }

    /** Tao RegisterRequest voi username tuy chinh. */
    public static RegisterRequest aRegisterRequest(String username) {
        RegisterRequest r = new RegisterRequest();
        r.setUsername(username);
        r.setPassword(TEST_PASSWORD);
        return r;
    }

    /** Tao LoginRequest mac dinh (password auth). */
    public static LoginRequest aLoginRequest() {
        LoginRequest r = new LoginRequest();
        r.setUsername(TEST_USERNAME);
        r.setPassword(TEST_PASSWORD);
        return r;
    }

    /** Tao User mac dinh (USER role, encoded password). */
    public static User aUser() {
        User u = new User();
        u.setId(1L);
        u.setUsername(TEST_USERNAME);
        u.setPassword(TEST_ENCODED_PASSWORD);
        u.setRole(Role.ROLE_USER);
        return u;
    }

    /** Tao User voi role tuy chinh. */
    public static User aUserWithRole(Role role) {
        User u = aUser();
        u.setRole(role);
        return u;
    }
}

package com.r2s.user.testdata;

import com.r2s.user.dto.UpdateUserRequest;
import com.r2s.user.entity.Role;
import com.r2s.user.entity.User;

/**
 * Builder Pattern de tao test data (DRY).
 */
public final class TestDataBuilder {

    public static final String TEST_USERNAME = "john";
    public static final String TEST_FULLNAME = "John Doe";
    public static final String TEST_EMAIL = "john@test.com";

    private TestDataBuilder() {}

    public static User aUser() {
        User u = new User();
        u.setId(1L);
        u.setUsername(TEST_USERNAME);
        u.setFullName(TEST_FULLNAME);
        u.setEmail(TEST_EMAIL);
        u.setRole(Role.ROLE_USER);
        return u;
    }

    public static User aUser(String username) {
        User u = aUser();
        u.setUsername(username);
        return u;
    }

    public static User anAdminUser() {
        User u = aUser("jane");
        u.setId(2L);
        u.setFullName("Jane Doe");
        u.setEmail("jane@test.com");
        u.setRole(Role.ROLE_ADMIN);
        return u;
    }

    public static UpdateUserRequest anUpdateRequest() {
        UpdateUserRequest r = new UpdateUserRequest();
        r.setFullName("John Updated");
        r.setEmail("john_new@test.com");
        return r;
    }

    public static UpdateUserRequest anUpdateRequest(String fullName, String email) {
        UpdateUserRequest r = new UpdateUserRequest();
        r.setFullName(fullName);
        r.setEmail(email);
        return r;
    }
}

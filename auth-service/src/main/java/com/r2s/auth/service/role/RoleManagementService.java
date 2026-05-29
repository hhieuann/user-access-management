package com.r2s.auth.service.role;

import com.r2s.auth.entity.Role;

/**
 * Interface chuyen biet cho admin role management.
 *
 * <p>Tach rieng theo ISP - day la admin-only operation, public controller
 * khong nen co reference den interface nay.
 */
public interface RoleManagementService {

    /**
     * Gan role cho user. Chi admin duoc goi.
     *
     * @throws org.springframework.security.access.AccessDeniedException neu admin tu doi role chinh minh
     * @throws IllegalArgumentException neu role null
     */
    void assignRole(String username, Role role);
}

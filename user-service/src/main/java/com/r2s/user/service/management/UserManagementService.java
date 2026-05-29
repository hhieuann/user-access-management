package com.r2s.user.service.management;

/**
 * Interface cho cac thao tac admin/management cua user (delete...).
 *
 * <p>Tach rieng tu UserProfileService theo nguyen tac ISP -
 * chi class can quyen admin (vd: AdminController, UserManagementController)
 * moi inject interface nay.
 */
public interface UserManagementService {

    /**
     * Xoa user khoi he thong. Chi admin duoc goi.
     *
     * @param username username can xoa
     * @throws org.springframework.security.core.userdetails.UsernameNotFoundException neu khong tim thay
     */
    void deleteUser(String username);
}

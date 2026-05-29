package com.r2s.user.service.profile;

import com.r2s.user.dto.UpdateUserRequest;
import com.r2s.user.dto.UserResponse;

import java.util.List;

/**
 * Interface cho cac thao tac profile cua user (read + update profile).
 *
 * <p>Tach rieng tu UserManagementService theo nguyen tac ISP -
 * client chi can xem/sua profile ca nhan KHONG bi buoc phu thuoc
 * vao cac thao tac admin nhu deleteUser.
 */
public interface UserProfileService {

    /** Lay danh sach toan bo user (Admin/Moderator endpoint). */
    List<UserResponse> getAllUsers();

    /** Lay profile cua user theo username. */
    UserResponse getUserByUsername(String username);

    /** Cap nhat profile cua user (fullName, email...). */
    UserResponse updateUser(String username, UpdateUserRequest request);
}

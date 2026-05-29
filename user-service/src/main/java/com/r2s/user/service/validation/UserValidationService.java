package com.r2s.user.service.validation;

import com.r2s.user.dto.UpdateUserRequest;

/**
 * Interface chuyen biet cho validation logic.
 *
 * <p>Tach rieng theo nguyen tac ISP - validation la concern doc lap
 * voi business logic. Cho phep dung chung tu nhieu noi:
 * - UserProfileServiceImpl khi update
 * - UserManagementServiceImpl khi delete (neu can validate)
 * - Test khi setup test data
 */
public interface UserValidationService {

    /**
     * Validate request update user. Throw exception neu invalid.
     *
     * @throws com.r2s.core.exception.BusinessException neu invalid
     */
    void validateUserUpdate(String username, UpdateUserRequest request);

    /**
     * Validate email format + uniqueness.
     *
     * @throws com.r2s.core.exception.DuplicateEmailException neu email da ton tai
     */
    void validateEmailUnique(String email, String currentUsername);
}

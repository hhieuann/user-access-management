package com.r2s.user.service;

import com.r2s.user.dto.UpdateUserRequest;
import com.r2s.user.dto.UserResponse;
import com.r2s.user.repository.UserRepository;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserService {

    private final UserRepository repo;

    public UserService(UserRepository repo) {
        this.repo = repo;
    }

    public List<UserResponse> getAllUsers() {
        return repo.findAll().stream()
                .map(UserResponse::fromEntity)
                .collect(Collectors.toList());
    }

    public UserResponse getUserByUsername(String username) {
        return repo.findByUsername(username)
                .map(UserResponse::fromEntity)
                .orElseThrow(() -> new UsernameNotFoundException("Not found"));
    }

    public UserResponse updateUser(String username, UpdateUserRequest req) {
        var user = repo.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Not found"));
        user.setFullName(req.getFullName());
        user.setEmail(req.getEmail());
        return UserResponse.fromEntity(repo.save(user));
    }

    public void deleteUser(String username) {
        repo.deleteByUsername(username);
    }
}
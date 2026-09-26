package com.ahmed.hospital.user.service;

import com.ahmed.hospital.common.exception.ResourceNotFoundException;
import com.ahmed.hospital.user.entity.User;
import com.ahmed.hospital.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public User findByEmail(String email) {

        return userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "User not found"
                        ));
    }
}
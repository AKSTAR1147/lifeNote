package com.akstar47.lifeNote.user.service;

import com.akstar47.lifeNote.common.exception.ResourceNotFoundException;
import com.akstar47.lifeNote.user.entity.AppUser;
import com.akstar47.lifeNote.user.repository.AppUserRepository;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthenticatedUserService {

    private final AppUserRepository userRepository;

    public AuthenticatedUserService(AppUserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public AppUser requireUser(UserDetails userDetails) {
        return userRepository.findByEmailIgnoreCase(userDetails.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user was not found"));
    }
}

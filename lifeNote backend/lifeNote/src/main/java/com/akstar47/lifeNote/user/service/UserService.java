package com.akstar47.lifeNote.user.service;

import com.akstar47.lifeNote.audit.dto.AuditAction;
import com.akstar47.lifeNote.audit.service.AuditService;
import com.akstar47.lifeNote.user.dto.UpdateProfileRequest;
import com.akstar47.lifeNote.user.dto.UserProfileResponse;
import com.akstar47.lifeNote.user.entity.AppUser;
import com.akstar47.lifeNote.user.mapper.UserMapper;
import com.akstar47.lifeNote.user.repository.AppUserRepository;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;
    private final AuditService auditService;

    public UserService(
            AppUserRepository userRepository,
            PasswordEncoder passwordEncoder,
            UserMapper userMapper,
            AuditService auditService
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.userMapper = userMapper;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public UserProfileResponse profile(AppUser user) {
        return userMapper.toProfileResponse(user);
    }

    @Transactional
    public UserProfileResponse updateProfile(AppUser user, UpdateProfileRequest request, HttpServletRequest httpRequest) {
        if (request.displayName() != null) {
            String displayName = request.displayName().trim();
            if (displayName.isBlank()) {
                throw new IllegalArgumentException("Display name cannot be blank");
            }
            user.setDisplayName(displayName);
        }

        if (request.password() != null) {
            user.setPasswordHash(passwordEncoder.encode(request.password()));
        }

        AppUser savedUser = userRepository.save(user);
        auditService.record(savedUser, AuditAction.PROFILE_UPDATE, "Updated profile", httpRequest);
        return userMapper.toProfileResponse(savedUser);
    }
}

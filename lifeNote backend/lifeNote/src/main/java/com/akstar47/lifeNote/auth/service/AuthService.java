package com.akstar47.lifeNote.auth.service;

import com.akstar47.lifeNote.audit.dto.AuditAction;
import com.akstar47.lifeNote.audit.service.AuditService;
import com.akstar47.lifeNote.auth.dto.AuthResponse;
import com.akstar47.lifeNote.auth.dto.LoginRequest;
import com.akstar47.lifeNote.auth.dto.RegisterRequest;
import com.akstar47.lifeNote.common.exception.ConflictException;
import com.akstar47.lifeNote.security.service.JwtService;
import com.akstar47.lifeNote.user.entity.AppUser;
import com.akstar47.lifeNote.user.mapper.UserMapper;
import com.akstar47.lifeNote.user.repository.AppUserRepository;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserMapper userMapper;
    private final AuditService auditService;
    private final long jwtExpirationMinutes;

    public AuthService(
            AppUserRepository userRepository,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager,
            JwtService jwtService,
            UserMapper userMapper,
            AuditService auditService,
            @Value("${lifenote.security.jwt-expiration-minutes}") long jwtExpirationMinutes
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.userMapper = userMapper;
        this.auditService = auditService;
        this.jwtExpirationMinutes = jwtExpirationMinutes;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request, HttpServletRequest httpRequest) {
        String email = normalizeEmail(request.email());
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new ConflictException("A user with this email already exists");
        }

        AppUser user = new AppUser(
                email,
                request.displayName().trim(),
                passwordEncoder.encode(request.password())
        );
        AppUser savedUser = userRepository.save(user);
        auditService.record(savedUser, AuditAction.REGISTRATION, "Registered LifeNote account", httpRequest);
        return authResponse(savedUser);
    }

    @Transactional
    public AuthResponse login(LoginRequest request, HttpServletRequest httpRequest) {
        String email = normalizeEmail(request.email());
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(email, request.password()));
        AppUser user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new IllegalArgumentException("Authenticated user was not found"));
        auditService.record(user, AuditAction.LOGIN, "Logged in", httpRequest);
        return authResponse(user);
    }

    private AuthResponse authResponse(AppUser user) {
        return new AuthResponse(
                "Bearer",
                jwtService.generateToken(user),
                jwtExpirationMinutes,
                userMapper.toProfileResponse(user)
        );
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}

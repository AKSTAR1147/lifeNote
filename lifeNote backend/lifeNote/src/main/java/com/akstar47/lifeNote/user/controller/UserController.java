package com.akstar47.lifeNote.user.controller;

import com.akstar47.lifeNote.user.dto.UpdateProfileRequest;
import com.akstar47.lifeNote.user.dto.UserProfileResponse;
import com.akstar47.lifeNote.user.service.AuthenticatedUserService;
import com.akstar47.lifeNote.user.service.UserService;

import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@PreAuthorize("hasAnyRole('USER', 'ADMIN')")
public class UserController {

    private final AuthenticatedUserService authenticatedUserService;
    private final UserService userService;

    public UserController(AuthenticatedUserService authenticatedUserService, UserService userService) {
        this.authenticatedUserService = authenticatedUserService;
        this.userService = userService;
    }

    @GetMapping("/me")
    public UserProfileResponse me(@AuthenticationPrincipal UserDetails userDetails) {
        return userService.profile(authenticatedUserService.requireUser(userDetails));
    }

    @PutMapping("/me")
    public UserProfileResponse updateMe(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody UpdateProfileRequest request,
            HttpServletRequest httpRequest
    ) {
        return userService.updateProfile(authenticatedUserService.requireUser(userDetails), request, httpRequest);
    }
}

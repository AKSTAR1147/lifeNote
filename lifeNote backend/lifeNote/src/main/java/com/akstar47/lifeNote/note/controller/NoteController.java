package com.akstar47.lifeNote.note.controller;

import com.akstar47.lifeNote.note.dto.DiffResponse;
import com.akstar47.lifeNote.note.dto.NoteCreateRequest;
import com.akstar47.lifeNote.note.dto.NoteFlagRequest;
import com.akstar47.lifeNote.note.dto.NoteResponse;
import com.akstar47.lifeNote.note.dto.NoteUpdateRequest;
import com.akstar47.lifeNote.note.dto.NoteVersionResponse;
import com.akstar47.lifeNote.note.service.NoteService;
import com.akstar47.lifeNote.user.entity.AppUser;
import com.akstar47.lifeNote.user.service.AuthenticatedUserService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notes")
@PreAuthorize("hasAnyRole('USER', 'ADMIN')")
public class NoteController {

    private final AuthenticatedUserService authenticatedUserService;
    private final NoteService noteService;

    public NoteController(AuthenticatedUserService authenticatedUserService, NoteService noteService) {
        this.authenticatedUserService = authenticatedUserService;
        this.noteService = noteService;
    }

    @PostMapping
    public NoteResponse create(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody NoteCreateRequest request,
            HttpServletRequest httpRequest
    ) {
        return noteService.create(currentUser(userDetails), request, httpRequest);
    }

    @GetMapping
    public List<NoteResponse> list(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "false") boolean includeArchived
    ) {
        return noteService.list(currentUser(userDetails), q, includeArchived);
    }

    @GetMapping("/{noteId}")
    public NoteResponse get(@AuthenticationPrincipal UserDetails userDetails, @PathVariable UUID noteId) {
        return noteService.get(currentUser(userDetails), noteId);
    }

    @PutMapping("/{noteId}")
    public NoteResponse update(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID noteId,
            @Valid @RequestBody NoteUpdateRequest request,
            HttpServletRequest httpRequest
    ) {
        return noteService.update(currentUser(userDetails), noteId, request, httpRequest);
    }

    @DeleteMapping("/{noteId}")
    public void delete(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID noteId,
            HttpServletRequest httpRequest
    ) {
        noteService.delete(currentUser(userDetails), noteId, httpRequest);
    }

    @PostMapping("/{noteId}/archive")
    public NoteResponse archive(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID noteId,
            HttpServletRequest httpRequest
    ) {
        return noteService.archive(currentUser(userDetails), noteId, httpRequest);
    }

    @PostMapping("/{noteId}/restore")
    public NoteResponse restoreArchived(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID noteId,
            HttpServletRequest httpRequest
    ) {
        return noteService.restoreArchived(currentUser(userDetails), noteId, httpRequest);
    }

    @PatchMapping("/{noteId}/pin")
    public NoteResponse pin(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID noteId,
            @RequestBody NoteFlagRequest request,
            HttpServletRequest httpRequest
    ) {
        return noteService.setPinned(currentUser(userDetails), noteId, request.enabled(), httpRequest);
    }

    @PatchMapping("/{noteId}/favorite")
    public NoteResponse favorite(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID noteId,
            @RequestBody NoteFlagRequest request,
            HttpServletRequest httpRequest
    ) {
        return noteService.setFavorite(currentUser(userDetails), noteId, request.enabled(), httpRequest);
    }

    @GetMapping("/{noteId}/versions")
    public List<NoteVersionResponse> history(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID noteId
    ) {
        return noteService.history(currentUser(userDetails), noteId);
    }

    @GetMapping("/{noteId}/versions/{versionNumber}")
    public NoteVersionResponse version(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID noteId,
            @PathVariable int versionNumber
    ) {
        return noteService.version(currentUser(userDetails), noteId, versionNumber);
    }

    @PostMapping("/{noteId}/versions/{versionNumber}/restore")
    public NoteResponse restoreVersion(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID noteId,
            @PathVariable int versionNumber,
            HttpServletRequest httpRequest
    ) {
        return noteService.restoreVersion(currentUser(userDetails), noteId, versionNumber, httpRequest);
    }

    @GetMapping("/{noteId}/diff")
    public DiffResponse diff(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID noteId,
            @RequestParam int from,
            @RequestParam int to
    ) {
        return noteService.diff(currentUser(userDetails), noteId, from, to);
    }

    @GetMapping("/versions/search")
    public List<NoteVersionResponse> searchVersions(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false) String q
    ) {
        return noteService.searchVersions(currentUser(userDetails), q);
    }

    private AppUser currentUser(UserDetails userDetails) {
        return authenticatedUserService.requireUser(userDetails);
    }
}

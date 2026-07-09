package com.akstar47.lifeNote.note.service;

import com.akstar47.lifeNote.audit.dto.AuditAction;
import com.akstar47.lifeNote.audit.service.AuditService;
import com.akstar47.lifeNote.common.exception.ResourceNotFoundException;
import com.akstar47.lifeNote.note.dto.DiffResponse;
import com.akstar47.lifeNote.note.dto.NoteCreateRequest;
import com.akstar47.lifeNote.note.dto.NoteResponse;
import com.akstar47.lifeNote.note.dto.NoteUpdateRequest;
import com.akstar47.lifeNote.note.dto.NoteVersionResponse;
import com.akstar47.lifeNote.note.entity.Note;
import com.akstar47.lifeNote.note.entity.NoteVersion;
import com.akstar47.lifeNote.note.mapper.NoteMapper;
import com.akstar47.lifeNote.note.repository.NoteRepository;
import com.akstar47.lifeNote.note.repository.NoteVersionRepository;
import com.akstar47.lifeNote.user.entity.AppUser;

import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NoteService {

    private final NoteRepository noteRepository;
    private final NoteVersionRepository noteVersionRepository;
    private final NoteMapper noteMapper;
    private final DiffService diffService;
    private final AuditService auditService;

    public NoteService(
            NoteRepository noteRepository,
            NoteVersionRepository noteVersionRepository,
            NoteMapper noteMapper,
            DiffService diffService,
            AuditService auditService
    ) {
        this.noteRepository = noteRepository;
        this.noteVersionRepository = noteVersionRepository;
        this.noteMapper = noteMapper;
        this.diffService = diffService;
        this.auditService = auditService;
    }

    @Transactional
    public NoteResponse create(AppUser owner, NoteCreateRequest request, HttpServletRequest httpRequest) {
        Note note = new Note(
                owner,
                requiredText(request.title(), "Title"),
                requiredText(request.content(), "Content"),
                nullableText(request.category()),
                normalizeTags(request.tags())
        );
        note.setPinned(request.pinned());
        note.setFavorite(request.favorite());

        Note savedNote = noteRepository.save(note);
        createVersion(savedNote, nullableText(request.commitMessage()));
        auditService.record(owner, AuditAction.NOTE_CREATED, "Created note " + savedNote.getId(), httpRequest);
        return toResponse(savedNote);
    }

    @Transactional(readOnly = true)
    public List<NoteResponse> list(AppUser owner, String query, boolean includeArchived) {
        String normalizedQuery = normalizeSearch(query);
        List<Note> workspaceNotes = includeArchived
                ? noteRepository.findByOwner_IdOrderByPinnedDescUpdatedAtDesc(owner.getId())
                : noteRepository.findByOwner_IdAndArchivedFalseOrderByPinnedDescUpdatedAtDesc(owner.getId());

        return workspaceNotes
                .stream()
                .filter(note -> matches(note, normalizedQuery))
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public NoteResponse get(AppUser owner, UUID noteId) {
        return toResponse(requireOwnedNote(owner, noteId));
    }

    @Transactional
    public NoteResponse update(AppUser owner, UUID noteId, NoteUpdateRequest request, HttpServletRequest httpRequest) {
        Note note = requireOwnedNote(owner, noteId);

        // Capture current content values before updates are applied
        String oldTitle = note.getTitle();
        String oldContent = note.getContent();
        String oldCategory = note.getCategory();
        Set<String> oldTags = new LinkedHashSet<>(note.getTags());

        if (request.title() != null) {
            note.setTitle(requiredText(request.title(), "Title"));
        }
        if (request.content() != null) {
            note.setContent(requiredText(request.content(), "Content"));
        }
        if (request.category() != null) {
            note.setCategory(nullableText(request.category()));
        }
        if (request.tags() != null) {
            note.replaceTags(normalizeTags(request.tags()));
        }
        if (request.pinned() != null) {
            note.setPinned(request.pinned());
        }
        if (request.favorite() != null) {
            note.setFavorite(request.favorite());
        }

        // Determine if any version-controlled content fields changed
        boolean hasContentChanges = !Objects.equals(oldTitle, note.getTitle())
                || !Objects.equals(oldContent, note.getContent())
                || !Objects.equals(oldCategory, note.getCategory())
                || !Objects.equals(oldTags, note.getTags());

        Note savedNote = noteRepository.save(note);
        
        if (hasContentChanges) {
            createVersion(savedNote, nullableText(request.commitMessage()));
            auditService.record(owner, AuditAction.NOTE_UPDATED, "Updated note " + savedNote.getId(), httpRequest);
        } else {
            auditService.record(owner, AuditAction.NOTE_UPDATED, "Updated metadata for note " + savedNote.getId(), httpRequest);
        }
        return toResponse(savedNote);
    }

    @Transactional
    public NoteResponse archive(AppUser owner, UUID noteId, HttpServletRequest httpRequest) {
        Note note = requireOwnedNote(owner, noteId);
        note.setArchived(true);
        auditService.record(owner, AuditAction.NOTE_ARCHIVED, "Archived note " + note.getId(), httpRequest);
        return toResponse(noteRepository.save(note));
    }

    @Transactional
    public NoteResponse restoreArchived(AppUser owner, UUID noteId, HttpServletRequest httpRequest) {
        Note note = requireOwnedNote(owner, noteId);
        note.setArchived(false);
        auditService.record(owner, AuditAction.NOTE_RESTORED, "Restored archived note " + note.getId(), httpRequest);
        return toResponse(noteRepository.save(note));
    }

    @Transactional
    public void delete(AppUser owner, UUID noteId, HttpServletRequest httpRequest) {
        Note note = requireOwnedNote(owner, noteId);
        noteVersionRepository.deleteByNote_Id(note.getId());
        noteRepository.delete(note);
        auditService.record(owner, AuditAction.NOTE_DELETED, "Deleted note " + noteId, httpRequest);
    }

    @Transactional
    public NoteResponse setPinned(AppUser owner, UUID noteId, boolean enabled, HttpServletRequest httpRequest) {
        Note note = requireOwnedNote(owner, noteId);
        note.setPinned(enabled);
        auditService.record(owner, AuditAction.NOTE_PINNED, "Set note pinned=" + enabled + " for " + noteId, httpRequest);
        return toResponse(noteRepository.save(note));
    }

    @Transactional
    public NoteResponse setFavorite(AppUser owner, UUID noteId, boolean enabled, HttpServletRequest httpRequest) {
        Note note = requireOwnedNote(owner, noteId);
        note.setFavorite(enabled);
        auditService.record(owner, AuditAction.NOTE_FAVORITED, "Set note favorite=" + enabled + " for " + noteId, httpRequest);
        return toResponse(noteRepository.save(note));
    }

    @Transactional(readOnly = true)
    public List<NoteVersionResponse> history(AppUser owner, UUID noteId) {
        requireOwnedNote(owner, noteId);
        return noteVersionRepository.findByNote_Owner_IdAndNote_IdOrderByVersionNumberDesc(owner.getId(), noteId)
                .stream()
                .map(noteMapper::toVersionResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public NoteVersionResponse version(AppUser owner, UUID noteId, int versionNumber) {
        return noteMapper.toVersionResponse(requireOwnedVersion(owner, noteId, versionNumber));
    }

    @Transactional(readOnly = true)
    public List<NoteVersionResponse> searchVersions(AppUser owner, String query) {
        String normalizedQuery = normalizeSearch(query);
        return noteVersionRepository.findByNote_Owner_IdOrderByCreatedAtDesc(owner.getId())
                .stream()
                .filter(version -> matches(version, normalizedQuery))
                .map(noteMapper::toVersionResponse)
                .toList();
    }

    @Transactional
    public NoteResponse restoreVersion(
            AppUser owner,
            UUID noteId,
            int versionNumber,
            HttpServletRequest httpRequest
    ) {
        NoteVersion version = requireOwnedVersion(owner, noteId, versionNumber);
        Note note = version.getNote();
        note.setTitle(version.getTitleSnapshot());
        note.setContent(version.getContentSnapshot());
        note.setCategory(version.getCategorySnapshot());
        note.replaceTags(version.getTagsSnapshot());
        note.setArchived(false);

        Note savedNote = noteRepository.save(note);
        createVersion(savedNote, "Restored version " + versionNumber);
        auditService.record(owner, AuditAction.VERSION_RESTORED, "Restored version " + versionNumber + " for note " + noteId, httpRequest);
        return toResponse(savedNote);
    }

    @Transactional(readOnly = true)
    public DiffResponse diff(AppUser owner, UUID noteId, int fromVersion, int toVersion) {
        NoteVersion from = requireOwnedVersion(owner, noteId, fromVersion);
        NoteVersion to = requireOwnedVersion(owner, noteId, toVersion);
        return new DiffResponse(
                noteId,
                fromVersion,
                toVersion,
                diffService.compare(from.getContentSnapshot(), to.getContentSnapshot())
        );
    }

    private Note requireOwnedNote(AppUser owner, UUID noteId) {
        return noteRepository.findByOwner_IdAndId(owner.getId(), noteId)
                .orElseThrow(() -> new ResourceNotFoundException("Note was not found in your workspace"));
    }

    private NoteVersion requireOwnedVersion(AppUser owner, UUID noteId, int versionNumber) {
        return noteVersionRepository.findByNote_Owner_IdAndNote_IdAndVersionNumber(owner.getId(), noteId, versionNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Version was not found in your workspace"));
    }

    private void createVersion(Note note, String commitMessage) {
        int currentVersion = noteVersionRepository.findCurrentVersionNumber(note.getId());
        int nextVersion = currentVersion + 1;
        Integer parentVersion = currentVersion == 0 ? null : currentVersion;
        noteVersionRepository.save(new NoteVersion(
                note,
                nextVersion,
                parentVersion,
                note.getTitle(),
                note.getContent(),
                note.getCategory(),
                new LinkedHashSet<>(note.getTags()),
                commitMessage
        ));
    }

    private NoteResponse toResponse(Note note) {
        int currentVersion = noteVersionRepository.findCurrentVersionNumber(note.getId());
        return noteMapper.toResponse(note, currentVersion);
    }

    private String requiredText(String value, String fieldName) {
        if (value == null || value.trim().isBlank()) {
            throw new IllegalArgumentException(fieldName + " cannot be blank");
        }
        return value.trim();
    }

    private String nullableText(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isBlank() ? null : trimmed;
    }

    private Set<String> normalizeTags(Set<String> tags) {
        if (tags == null) {
            return Set.of();
        }
        return tags.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(tag -> !tag.isBlank())
                .map(tag -> tag.toLowerCase(Locale.ROOT))
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private String normalizeSearch(String query) {
        String normalized = nullableText(query);
        return normalized == null ? null : normalized.toLowerCase(Locale.ROOT);
    }

    private boolean matches(Note note, String query) {
        return query == null
                || contains(note.getTitle(), query)
                || contains(note.getContent(), query)
                || contains(note.getCategory(), query)
                || note.getTags().stream().anyMatch(tag -> contains(tag, query));
    }

    private boolean matches(NoteVersion version, String query) {
        return query == null
                || contains(version.getTitleSnapshot(), query)
                || contains(version.getContentSnapshot(), query)
                || contains(version.getCategorySnapshot(), query)
                || version.getTagsSnapshot().stream().anyMatch(tag -> contains(tag, query));
    }

    private boolean contains(String value, String query) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(query);
    }
}

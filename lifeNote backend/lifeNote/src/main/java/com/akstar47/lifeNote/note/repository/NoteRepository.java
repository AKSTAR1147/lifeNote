package com.akstar47.lifeNote.note.repository;

import com.akstar47.lifeNote.note.entity.Note;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NoteRepository extends JpaRepository<Note, UUID> {

    Optional<Note> findByOwner_IdAndId(UUID ownerId, UUID id);

    List<Note> findByOwner_IdOrderByPinnedDescUpdatedAtDesc(UUID ownerId);

    List<Note> findByOwner_IdAndArchivedFalseOrderByPinnedDescUpdatedAtDesc(UUID ownerId);
}

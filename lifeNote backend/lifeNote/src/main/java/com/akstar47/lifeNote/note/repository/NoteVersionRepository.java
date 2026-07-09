package com.akstar47.lifeNote.note.repository;

import com.akstar47.lifeNote.note.entity.NoteVersion;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NoteVersionRepository extends JpaRepository<NoteVersion, UUID> {

    List<NoteVersion> findByNote_Owner_IdAndNote_IdOrderByVersionNumberDesc(UUID ownerId, UUID noteId);

    Optional<NoteVersion> findByNote_Owner_IdAndNote_IdAndVersionNumber(UUID ownerId, UUID noteId, int versionNumber);

    List<NoteVersion> findByNote_Owner_IdOrderByCreatedAtDesc(UUID ownerId);

    void deleteByNote_Id(UUID noteId);

    @Query("select coalesce(max(v.versionNumber), 0) from NoteVersion v where v.note.id = :noteId")
    int findCurrentVersionNumber(@Param("noteId") UUID noteId);
}

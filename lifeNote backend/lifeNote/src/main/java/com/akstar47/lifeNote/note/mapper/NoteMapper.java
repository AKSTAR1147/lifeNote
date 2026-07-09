package com.akstar47.lifeNote.note.mapper;

import com.akstar47.lifeNote.note.dto.NoteResponse;
import com.akstar47.lifeNote.note.dto.NoteVersionResponse;
import com.akstar47.lifeNote.note.entity.Note;
import com.akstar47.lifeNote.note.entity.NoteVersion;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface NoteMapper {

    @Mapping(target = "currentVersion", source = "currentVersion")
    NoteResponse toResponse(Note note, int currentVersion);

    @Mapping(target = "noteId", source = "note.id")
    @Mapping(target = "title", source = "titleSnapshot")
    @Mapping(target = "content", source = "contentSnapshot")
    @Mapping(target = "category", source = "categorySnapshot")
    @Mapping(target = "tags", source = "tagsSnapshot")
    NoteVersionResponse toVersionResponse(NoteVersion noteVersion);
}

package com.akstar47.lifeNote.note.service;

import com.akstar47.lifeNote.note.dto.ChangeType;
import com.akstar47.lifeNote.note.dto.DiffLineResponse;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class DiffService {

    public List<DiffLineResponse> compare(String oldContent, String newContent) {
        List<String> oldLines = lines(oldContent);
        List<String> newLines = lines(newContent);
        List<DiffOperation> operations = operations(oldLines, newLines);
        return collapseModifications(operations);
    }

    private List<String> lines(String content) {
        if (content == null || content.isEmpty()) {
            return List.of();
        }
        return Arrays.asList(content.split("\\R", -1));
    }

    private List<DiffOperation> operations(List<String> oldLines, List<String> newLines) {
        int[][] lcs = new int[oldLines.size() + 1][newLines.size() + 1];
        for (int i = oldLines.size() - 1; i >= 0; i--) {
            for (int j = newLines.size() - 1; j >= 0; j--) {
                if (oldLines.get(i).equals(newLines.get(j))) {
                    lcs[i][j] = lcs[i + 1][j + 1] + 1;
                } else {
                    lcs[i][j] = Math.max(lcs[i + 1][j], lcs[i][j + 1]);
                }
            }
        }

        List<DiffOperation> operations = new ArrayList<>();
        int i = 0;
        int j = 0;
        while (i < oldLines.size() && j < newLines.size()) {
            if (oldLines.get(i).equals(newLines.get(j))) {
                operations.add(DiffOperation.unchanged(i + 1, j + 1, oldLines.get(i)));
                i++;
                j++;
            } else if (lcs[i + 1][j] >= lcs[i][j + 1]) {
                operations.add(DiffOperation.removed(i + 1, oldLines.get(i)));
                i++;
            } else {
                operations.add(DiffOperation.added(j + 1, newLines.get(j)));
                j++;
            }
        }

        while (i < oldLines.size()) {
            operations.add(DiffOperation.removed(i + 1, oldLines.get(i)));
            i++;
        }

        while (j < newLines.size()) {
            operations.add(DiffOperation.added(j + 1, newLines.get(j)));
            j++;
        }

        return operations;
    }

    private List<DiffLineResponse> collapseModifications(List<DiffOperation> operations) {
        List<DiffLineResponse> changes = new ArrayList<>();
        int index = 0;
        while (index < operations.size()) {
            DiffOperation current = operations.get(index);
            if (current.changeType == null) {
                index++;
                continue;
            }

            if (current.changeType == ChangeType.REMOVED && index + 1 < operations.size()) {
                DiffOperation next = operations.get(index + 1);
                if (next.changeType == ChangeType.ADDED) {
                    changes.add(new DiffLineResponse(
                            ChangeType.MODIFIED,
                            current.oldLineNumber,
                            next.newLineNumber,
                            current.text,
                            next.text
                    ));
                    index += 2;
                    continue;
                }
            }

            if (current.changeType == ChangeType.REMOVED) {
                changes.add(new DiffLineResponse(ChangeType.REMOVED, current.oldLineNumber, null, current.text, null));
            } else if (current.changeType == ChangeType.ADDED) {
                changes.add(new DiffLineResponse(ChangeType.ADDED, null, current.newLineNumber, null, current.text));
            }
            index++;
        }
        return changes;
    }

    private record DiffOperation(
            ChangeType changeType,
            Integer oldLineNumber,
            Integer newLineNumber,
            String text
    ) {
        static DiffOperation unchanged(int oldLineNumber, int newLineNumber, String text) {
            return new DiffOperation(null, oldLineNumber, newLineNumber, text);
        }

        static DiffOperation removed(int oldLineNumber, String text) {
            return new DiffOperation(ChangeType.REMOVED, oldLineNumber, null, text);
        }

        static DiffOperation added(int newLineNumber, String text) {
            return new DiffOperation(ChangeType.ADDED, null, newLineNumber, text);
        }
    }
}

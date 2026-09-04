package com.imagedupmanager.service;

/**
 * Result of validating a rename before execution (preview), shown to the user for
 * confirmation. Renaming never overwrites silently (AGENTS.md #36).
 */
public final class RenamePreview {

    private final Long imageId;
    private final String currentName;
    private final String currentPath;
    private final String newName;
    private final String newPath;

    public RenamePreview(Long imageId, String currentName, String currentPath,
                         String newName, String newPath) {
        this.imageId = imageId;
        this.currentName = currentName;
        this.currentPath = currentPath;
        this.newName = newName;
        this.newPath = newPath;
    }

    public Long getImageId() {
        return imageId;
    }

    public String getCurrentName() {
        return currentName;
    }

    public String getCurrentPath() {
        return currentPath;
    }

    public String getNewName() {
        return newName;
    }

    public String getNewPath() {
        return newPath;
    }
}

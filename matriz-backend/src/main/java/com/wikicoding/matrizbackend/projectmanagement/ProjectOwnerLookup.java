package com.wikicoding.matrizbackend.projectmanagement;

import java.util.UUID;

/** Resolves the active local profile for an authenticated identity. */
public interface ProjectOwnerLookup {
    UUID requireActiveOwnerId(String subject);
}

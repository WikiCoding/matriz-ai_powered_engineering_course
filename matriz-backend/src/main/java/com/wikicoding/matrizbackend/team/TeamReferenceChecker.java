package com.wikicoding.matrizbackend.team;

import java.util.UUID;

/** Public cross-module query for records that prevent a team from being deleted. */
public interface TeamReferenceChecker {

    boolean hasReferenceTo(UUID teamId);
}

package com.libris.user.event;

/** The account just reached the overdue limit and lost its borrowing rights. */
public record AccountBlockedEvent(Long userId) {
}

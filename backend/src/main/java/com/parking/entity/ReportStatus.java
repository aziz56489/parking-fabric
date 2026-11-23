package com.parking.entity;

/** What the admin decided about a non paying report. */
public enum ReportStatus {
    /** Not reviewed yet. */
    PENDING,
    /** The admin banned (blocked) the reported user. */
    BANNED,
    /** The admin let it go: the user is not banned. */
    DISMISSED
}

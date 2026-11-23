package com.parking.entity;

public enum AppealStatus {
    /** Waiting for an admin. */
    PENDING,
    /** The admin agreed: the user is unbanned. */
    ACCEPTED,
    /** The admin kept the ban. The user may appeal again. */
    REJECTED
}

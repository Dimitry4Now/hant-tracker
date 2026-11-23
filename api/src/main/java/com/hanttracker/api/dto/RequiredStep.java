package com.hanttracker.api.dto;

/**
 * What a signed-in player has to do before the rest of the app opens up. The
 * API answers 428 with this to anything else (see AccountSetupFilter).
 */
public enum RequiredStep {
    /** Still on the old default password, or one an admin set. */
    CHANGE_PASSWORD,
    /** Hasn't confirmed a real email yet; only asked once the server can send email. */
    CONFIRM_EMAIL
}

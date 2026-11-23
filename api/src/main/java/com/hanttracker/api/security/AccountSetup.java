package com.hanttracker.api.security;

import com.hanttracker.api.domain.UserAccount;
import com.hanttracker.api.dto.RequiredStep;
import com.hanttracker.api.service.Mailer;
import org.springframework.stereotype.Component;

/** Decides what a player must do before they can use the app. */
@Component
public class AccountSetup {

    private final Mailer mailer;

    public AccountSetup(Mailer mailer) {
        this.mailer = mailer;
    }

    /** The step this player has to take first, or null when there is none. */
    public RequiredStep requiredStep(UserAccount user) {
        if (user.isMustChangePassword()) {
            return RequiredStep.CHANGE_PASSWORD;
        }
        // Many accounts were made with made-up emails; a reset link needs a real one.
        if (mailer.enabled() && !user.isEmailVerified()) {
            return RequiredStep.CONFIRM_EMAIL;
        }
        return null;
    }
}

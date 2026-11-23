package com.hanttracker.api.security;

import com.hanttracker.api.domain.UserAccount;
import com.hanttracker.api.dto.RequiredStep;
import org.springframework.stereotype.Component;

/** Decides what a player must do before they can use the app. */
@Component
public class AccountSetup {

    /** The step this player has to take first, or null when there is none. */
    public RequiredStep requiredStep(UserAccount user) {
        if (user.isMustChangePassword()) {
            return RequiredStep.CHANGE_PASSWORD;
        }
        return null;
    }
}

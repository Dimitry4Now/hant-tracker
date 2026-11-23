-- Whether the player confirmed their email with a code, so reset links may
-- be sent to it.
ALTER TABLE app_user ADD COLUMN email_verified BOOLEAN NOT NULL DEFAULT FALSE;

-- A code sent to confirm an email address; one at a time per user.
CREATE TABLE email_code (
    user_id    BIGINT                   PRIMARY KEY REFERENCES app_user (id),
    email      VARCHAR(255)             NOT NULL,
    code_hash  VARCHAR(64)              NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    attempts   INTEGER                  NOT NULL
);

-- A password reset link; only the SHA-256 of its token is stored.
CREATE TABLE password_reset (
    token_hash VARCHAR(64)              PRIMARY KEY,
    user_id    BIGINT                   NOT NULL REFERENCES app_user (id),
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL
);
CREATE INDEX password_reset_user ON password_reset (user_id);

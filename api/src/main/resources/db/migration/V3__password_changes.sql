-- Accounts that must pick a new password before doing anything else (the old
-- default password, or one an admin set), and a version that every login
-- token carries, so changing a password signs out older sessions.
ALTER TABLE app_user ADD COLUMN must_change_password BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE app_user ADD COLUMN token_version INTEGER NOT NULL DEFAULT 0;

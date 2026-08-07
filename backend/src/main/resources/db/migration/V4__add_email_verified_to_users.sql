ALTER TABLE users
    ADD COLUMN email_verified BOOLEAN NOT NULL DEFAULT FALSE;

-- Existing Google-authenticated accounts already proved email ownership via the provider.
UPDATE users SET email_verified = TRUE WHERE auth_provider = 'GOOGLE';

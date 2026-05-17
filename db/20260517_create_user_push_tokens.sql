CREATE TABLE IF NOT EXISTS user_push_tokens (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token TEXT NOT NULL UNIQUE,
    platform VARCHAR(20) NOT NULL,
    device_id VARCHAR(100),
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    last_seen_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'user_push_tokens' AND column_name = 'fcm_token'
    ) AND NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'user_push_tokens' AND column_name = 'token'
    ) THEN
        ALTER TABLE user_push_tokens RENAME COLUMN fcm_token TO token;
    END IF;

    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'user_push_tokens' AND column_name = 'is_active'
    ) AND NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'user_push_tokens' AND column_name = 'enabled'
    ) THEN
        ALTER TABLE user_push_tokens RENAME COLUMN is_active TO enabled;
    END IF;
END $$;

ALTER TABLE user_push_tokens
    ADD COLUMN IF NOT EXISTS token TEXT,
    ADD COLUMN IF NOT EXISTS enabled BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW();

ALTER TABLE user_push_tokens
    ALTER COLUMN device_id DROP NOT NULL;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conrelid = 'user_push_tokens'::regclass
          AND conname = 'user_push_tokens_token_key'
    ) THEN
        ALTER TABLE user_push_tokens
            ADD CONSTRAINT user_push_tokens_token_key UNIQUE (token);
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_user_push_tokens_user_enabled
    ON user_push_tokens(user_id, enabled);

-- Table refresh_tokens
-- À ajouter à votre script de migration / schema.sql

CREATE TABLE IF NOT EXISTS refresh_tokens (
    id         VARCHAR(36)  NOT NULL PRIMARY KEY,
    token      VARCHAR(210) NOT NULL UNIQUE,
    user_id    VARCHAR(36)  NOT NULL,
    expires_at DATETIME     NOT NULL,
    revoked    TINYINT(1)   NOT NULL DEFAULT 0,
    created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_refresh_token_user
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,

    INDEX idx_token    (token),
    INDEX idx_user_id  (user_id),
    INDEX idx_expires  (expires_at)
);
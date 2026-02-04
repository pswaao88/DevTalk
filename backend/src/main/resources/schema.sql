DROP TABLE IF EXISTS message;
DROP TABLE IF EXISTS session;

CREATE TABLE session (
                         session_id            VARCHAR(36)  NOT NULL PRIMARY KEY,
                         title                 VARCHAR(255) NOT NULL,
                         status                VARCHAR(20)  NOT NULL,
                         description           TEXT,
                         ai_summary            TEXT,
                         last_summarized_index INT DEFAULT 0, -- [추가] 요약 위치 기록용
                         created_at            DATETIME(6)  NOT NULL,
                         last_updated_at       DATETIME(6)  NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE message (
                         message_id         VARCHAR(36)  NOT NULL PRIMARY KEY,
                         session_id         VARCHAR(36)  NOT NULL,
                         role               VARCHAR(20)  NOT NULL,
                         content            LONGTEXT     NOT NULL,
                         status             VARCHAR(20)  NOT NULL,
                         markers            TEXT,

    -- Metadata
                         input_token_count  INT,
                         output_token_count INT,
                         latency_ms         BIGINT,
                         finish_reason      VARCHAR(50),

                         created_at         DATETIME(6)  NOT NULL,

                         CONSTRAINT fk_message_session
                             FOREIGN KEY (session_id) REFERENCES session (session_id)
                                 ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

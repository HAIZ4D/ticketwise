CREATE TABLE ticket_embeddings (
    ticket_id  BIGINT PRIMARY KEY REFERENCES tickets (id),
    model      VARCHAR(60)              NOT NULL,
    embedding  BYTEA                    NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

-- Existing requesters get an account that can never log in: '!' is not a valid BCrypt hash.
INSERT INTO users (email, password_hash, role, created_at)
SELECT DISTINCT LOWER(requester_email), '!', 'EMPLOYEE', CURRENT_TIMESTAMP
FROM tickets;

ALTER TABLE tickets ADD COLUMN requester_id BIGINT REFERENCES users (id);
ALTER TABLE tickets ADD COLUMN assignee_id BIGINT REFERENCES users (id);

UPDATE tickets
SET requester_id = (SELECT users.id FROM users WHERE users.email = LOWER(tickets.requester_email));

ALTER TABLE tickets ALTER COLUMN requester_id SET NOT NULL;
ALTER TABLE tickets DROP COLUMN requester_email;
ALTER TABLE tickets DROP COLUMN assignee_email;

CREATE INDEX idx_tickets_requester_id ON tickets (requester_id);
CREATE INDEX idx_tickets_assignee_id ON tickets (assignee_id);

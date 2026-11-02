-- Application-owned data only. IDs are assigned by the application.
CREATE TABLE team (
    id UUID PRIMARY KEY,
    name TEXT NOT NULL
);

CREATE TABLE app_user (
    id UUID PRIMARY KEY,
    keycloak_subject TEXT UNIQUE,
    email TEXT NOT NULL,
    team_id UUID REFERENCES team(id),
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    provisioning_status TEXT NOT NULL DEFAULT 'PENDING'
        CHECK (provisioning_status IN ('PENDING', 'COMPLETED')),
    provisioning_correlation_id UUID NOT NULL UNIQUE,
    CONSTRAINT completed_user_has_identity CHECK (
        deleted OR provisioning_status <> 'COMPLETED' OR keycloak_subject IS NOT NULL
    ),
    CONSTRAINT active_user_has_team CHECK (deleted OR team_id IS NOT NULL)
);
CREATE UNIQUE INDEX app_user_email_unique ON app_user (lower(email));
CREATE INDEX app_user_team_idx ON app_user (team_id);

CREATE TABLE project (
    id UUID PRIMARY KEY,
    name VARCHAR(100) NOT NULL CHECK (char_length(name) >= 3 AND name ~ '[^[:space:]]'),
    description VARCHAR(1000) NOT NULL,
    team_id UUID NOT NULL REFERENCES team(id),
    owner_id UUID NOT NULL REFERENCES app_user(id)
);
CREATE INDEX project_team_idx ON project (team_id);
CREATE INDEX project_owner_idx ON project (owner_id);

CREATE TABLE task (
    id UUID PRIMARY KEY,
    project_id UUID NOT NULL REFERENCES project(id) ON DELETE CASCADE,
    title VARCHAR(150) NOT NULL CHECK (char_length(title) >= 3 AND title ~ '[^[:space:]]'),
    description VARCHAR(2000) NOT NULL,
    effort SMALLINT NOT NULL CHECK (effort IN (1, 2, 3, 5, 8, 13)),
    status TEXT NOT NULL DEFAULT 'TODO' CHECK (status IN ('TODO', 'IN_PROGRESS', 'DONE')),
    priority TEXT NOT NULL DEFAULT 'MEDIUM' CHECK (priority IN ('LOW', 'MEDIUM', 'HIGH')),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL DEFAULT 0
);
CREATE INDEX task_project_status_priority_idx ON task (project_id, status, priority);
CREATE INDEX task_status_priority_idx ON task (status, priority);

CREATE TABLE task_assignment (
    task_id UUID PRIMARY KEY REFERENCES task(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES app_user(id),
    CONSTRAINT task_assignment_pair_unique UNIQUE (task_id, user_id)
);
CREATE INDEX task_assignment_user_idx ON task_assignment (user_id);

CREATE TABLE comment (
    id UUID PRIMARY KEY,
    task_id UUID NOT NULL REFERENCES task(id) ON DELETE CASCADE,
    author_id UUID NOT NULL REFERENCES app_user(id),
    content VARCHAR(2000) NOT NULL CHECK (content ~ '[^[:space:]]'),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);
CREATE INDEX comment_task_created_idx ON comment (task_id, created_at);
CREATE INDEX comment_author_idx ON comment (author_id);

-- Match the fixed subjects in config/keycloak/matriz-realm.json for fresh local imports.
INSERT INTO team (id, name)
VALUES ('e0a987ab-bff0-4de8-b320-04b1f42899aa', 'Demo Team');

INSERT INTO app_user (id, keycloak_subject, email, team_id, provisioning_status, provisioning_correlation_id)
VALUES
    ('c81ee4a9-292b-49fb-99bf-f5fd6b5cf5b5', 'c81ee4a9-292b-49fb-99bf-f5fd6b5cf5b5', 'admin@matriz.local', 'e0a987ab-bff0-4de8-b320-04b1f42899aa', 'COMPLETED', '13d5840a-a651-4c22-9da1-44632f7660ca'),
    ('16f3e507-4a2b-43a0-9ad1-df6ea61bb511', '16f3e507-4a2b-43a0-9ad1-df6ea61bb511', 'manager@matriz.local', 'e0a987ab-bff0-4de8-b320-04b1f42899aa', 'COMPLETED', '7b413d1a-4ecd-49a0-b148-f04217346dad'),
    ('d811ff06-d3ba-4dda-af09-f0ef1069f045', 'd811ff06-d3ba-4dda-af09-f0ef1069f045', 'engineer@matriz.local', 'e0a987ab-bff0-4de8-b320-04b1f42899aa', 'COMPLETED', 'ea613ae3-1c8b-4ef0-87f5-bba54e2c84cc');
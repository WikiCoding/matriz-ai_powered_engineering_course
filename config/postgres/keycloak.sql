-- Keycloak owns its tables and migrations. Run against the application database
-- so provisioning also works when the PostgreSQL volume already exists.
\getenv keycloak_db KEYCLOAK_DB_NAME
\getenv keycloak_user KEYCLOAK_DB_USER
\getenv keycloak_password KEYCLOAK_DB_PASSWORD

-- Fail before touching an application role/database if names overlap.
SELECT current_database() <> :'keycloak_db'
   AND current_user <> :'keycloak_user' AS separate_identity_storage \gset
\if :separate_identity_storage
SELECT format('CREATE ROLE %I LOGIN PASSWORD %L', :'keycloak_user', :'keycloak_password')
WHERE NOT EXISTS (SELECT FROM pg_roles WHERE rolname = :'keycloak_user')
\gexec

SELECT format('CREATE DATABASE %I OWNER %I', :'keycloak_db', :'keycloak_user')
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = :'keycloak_db')
\gexec
\else
\echo 'Keycloak must use a separate database and role.'
SELECT 1 / 0 AS invalid_identity_storage_configuration;
\endif

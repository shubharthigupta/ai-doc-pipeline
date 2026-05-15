-- PostgreSQL uses trust auth by default; configure md5 for password auth
ALTER SYSTEM SET password_encryption = 'scram-sha-256';
SELECT pg_reload_conf();

-- Set password for admin user (already created by POSTGRES_USER env var)
ALTER USER admin WITH PASSWORD 'secret';

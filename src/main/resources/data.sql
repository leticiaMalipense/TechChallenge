INSERT INTO public.users
    ("role", id, active, creation_date, email, "password", updated_date)
VALUES ('ADMIN', 1, true, '2026-10-06 23:58:57.295',
        'admin@admin', '$2a$10$kOvua.xd5tsEQmcT1nWlWOq5xjVrNbaXjb13lwy1gQ8KX0muQ2hhi',
        '2026-10-06 23:58:57.295')
ON CONFLICT (id) DO NOTHING;

ALTER SEQUENCE users_id_seq RESTART START WITH 2;
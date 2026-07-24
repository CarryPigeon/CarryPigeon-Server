-- Test data for local development and integration verification.
-- Assumption: schema has already been initialized by 00-all-in-one.sql
-- or by 01-auth.sql -> 06-plugin.sql.
--
-- Accounts created by this script:
--   carry-owner  / carrypigeon123
--   carry-admin  / carry-admin-123
--   carry-member / carry-member-123

START TRANSACTION;

DELETE FROM chat_notification_channel_preference
WHERE account_id IN (1001, 1002, 1003)
   OR channel_id IN (100, 101);

DELETE FROM chat_notification_server_preference
WHERE account_id IN (1001, 1002, 1003);

DELETE FROM chat_message_idempotency
WHERE account_id IN (1001, 1002, 1003)
   OR message_id IN (5001, 5002, 5003, 5004, 5005);

DELETE FROM chat_mention
WHERE mention_id IN (9001)
   OR message_id IN (5001, 5002, 5003, 5004, 5005)
   OR from_account_id IN (1001, 1002, 1003)
   OR target_account_id IN (1001, 1002, 1003)
   OR channel_id IN (100, 101);

DELETE FROM chat_channel_pin
WHERE pin_id IN (8001)
   OR message_id IN (5001, 5002, 5003, 5004, 5005)
   OR pinned_by_account_id IN (1001, 1002, 1003)
   OR channel_id IN (100, 101);

DELETE FROM chat_channel_read_state
WHERE account_id IN (1001, 1002, 1003)
   OR channel_id IN (100, 101);

DELETE FROM chat_channel_audit_log
WHERE audit_id IN (7001, 7002)
   OR actor_account_id IN (1001, 1002, 1003)
   OR target_account_id IN (1001, 1002, 1003)
   OR channel_id IN (100, 101);

DELETE FROM chat_channel_ban
WHERE channel_id IN (100, 101)
   OR banned_account_id IN (1001, 1002, 1003)
   OR operator_account_id IN (1001, 1002, 1003);

DELETE FROM chat_channel_invite
WHERE channel_id IN (100, 101)
   OR invitee_account_id IN (1001, 1002, 1003)
   OR inviter_account_id IN (1001, 1002, 1003)
   OR application_id IN (3001, 3002);

DELETE FROM chat_message
WHERE message_id IN (5001, 5002, 5003, 5004, 5005)
   OR sender_id IN (1001, 1002, 1003)
   OR channel_id IN (100, 101);

DELETE FROM chat_channel_member
WHERE account_id IN (1001, 1002, 1003)
   OR channel_id IN (100, 101);

DELETE FROM chat_channel
WHERE id IN (100, 101);

DELETE FROM user_profile
WHERE account_id IN (1001, 1002, 1003);

DELETE FROM auth_refresh_session
WHERE account_id IN (1001, 1002, 1003)
   OR id IN (2001, 2002, 2003);

DELETE FROM auth_account
WHERE id IN (1001, 1002, 1003);

INSERT INTO auth_account (
    id,
    username,
    password_hash,
    created_at,
    updated_at
)
VALUES
    (
        1001,
        'carry-owner',
        '$argon2id$v=19$m=16384,t=2,p=1$fwca8CKTOrsOw6zBIfP2AA$WYFBYwej/EotzgBH86aedU1HHJX7qYdfxQ6kB94MFGA',
        '2026-05-01 10:00:00.000000',
        '2026-05-01 10:00:00.000000'
    ),
    (
        1002,
        'carry-admin',
        '$argon2id$v=19$m=16384,t=2,p=1$B5O1A2MMpDIyLobdpnEt3A$PM0TuRtLrH242Uey4V5CaUdpCqHv4nxAmTm4OiUusoQ',
        '2026-05-01 10:01:00.000000',
        '2026-05-01 10:01:00.000000'
    ),
    (
        1003,
        'carry-member',
        '$argon2id$v=19$m=16384,t=2,p=1$OuIOxdDNfNDYmQRnDBPFVg$qTKnNWDVTawbyaAwUfZ6QpcObAGH39Rzd5Y4HgPHyhk',
        '2026-05-01 10:02:00.000000',
        '2026-05-01 10:02:00.000000'
    );

INSERT INTO auth_refresh_session (
    id,
    account_id,
    refresh_token_hash,
    expires_at,
    revoked,
    created_at,
    updated_at
)
VALUES
    (
        2001,
        1001,
        '111122223333444455556666777788889999aaaabbbbccccddddeeeeffff0000',
        '2026-06-30 10:00:00.000000',
        FALSE,
        '2026-05-01 10:05:00.000000',
        '2026-05-01 10:05:00.000000'
    ),
    (
        2002,
        1002,
        '0000ffffeeeeddddccccbbbbaaaa999988887777666655554444333322221111',
        '2026-06-30 10:01:00.000000',
        FALSE,
        '2026-05-01 10:06:00.000000',
        '2026-05-01 10:06:00.000000'
    ),
    (
        2003,
        1003,
        '1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef',
        '2026-06-30 10:02:00.000000',
        TRUE,
        '2026-05-01 10:07:00.000000',
        '2026-05-02 10:07:00.000000'
    );

INSERT INTO user_profile (
    account_id,
    nickname,
    avatar_url,
    bio,
    sex,
    birthday,
    created_at,
    updated_at
)
VALUES
    (
        1001,
        'Carry Owner',
        'https://example.invalid/avatar/carry-owner.png',
        'Project owner account for local development.',
        0,
        0,
        '2026-05-01 10:10:00.000000',
        '2026-05-01 10:10:00.000000'
    ),
    (
        1002,
        'Carry Admin',
        'https://example.invalid/avatar/carry-admin.png',
        'Admin account used for governance and moderation scenarios.',
        0,
        0,
        '2026-05-01 10:11:00.000000',
        '2026-05-01 10:11:00.000000'
    ),
    (
        1003,
        'Carry Member',
        'https://example.invalid/avatar/carry-member.png',
        'Member account used for mention, unread and notification tests.',
        0,
        0,
        '2026-05-01 10:12:00.000000',
        '2026-05-01 10:12:00.000000'
    );

INSERT INTO chat_channel (
    id,
    conversation_id,
    name,
    type,
    is_default,
    created_at,
    updated_at,
    brief,
    avatar
)
VALUES
    (
        100,
        100,
        'project-alpha',
        'private',
        FALSE,
        '2026-05-01 11:00:00',
        '2026-05-01 11:00:00',
        'Private workspace for project alpha.',
        'avatars/channels/project-alpha.png'
    ),
    (
        101,
        101,
        'frontend-lab',
        'public',
        FALSE,
        '2026-05-01 11:05:00',
        '2026-05-01 11:05:00',
        'Public channel for frontend experiments.',
        'avatars/channels/frontend-lab.png'
    );

INSERT INTO chat_channel_member (
    channel_id,
    account_id,
    role,
    joined_at,
    muted_until
)
VALUES
    (1, 1001, 'OWNER', '2026-05-01 11:10:00', NULL),
    (1, 1002, 'MEMBER', '2026-05-01 11:11:00', NULL),
    (1, 1003, 'MEMBER', '2026-05-01 11:12:00', NULL),
    (2, 1001, 'ADMIN', '2026-05-01 11:13:00', NULL),
    (100, 1001, 'OWNER', '2026-05-01 11:20:00', NULL),
    (100, 1002, 'ADMIN', '2026-05-01 11:21:00', NULL),
    (100, 1003, 'MEMBER', '2026-05-01 11:22:00', '2026-05-01 12:00:00'),
    (101, 1001, 'OWNER', '2026-05-01 11:23:00', NULL),
    (101, 1002, 'MEMBER', '2026-05-01 11:24:00', NULL);

INSERT INTO chat_channel_invite (
    channel_id,
    invitee_account_id,
    inviter_account_id,
    reason,
    status,
    created_at,
    responded_at,
    application_id
)
VALUES
    (
        101,
        1003,
        1001,
        '想加入项目协作频道',
        'PENDING',
        '2026-05-01 11:30:00',
        NULL,
        3001
    ),
    (
        100,
        1003,
        1001,
        NULL,
        'ACCEPTED',
        '2026-05-01 11:31:00',
        '2026-05-01 11:32:00',
        3002
    );

INSERT INTO chat_channel_ban (
    channel_id,
    banned_account_id,
    operator_account_id,
    reason,
    expires_at,
    created_at,
    revoked_at
)
VALUES
    (
        101,
        1003,
        1001,
        'spam links',
        NULL,
        '2026-05-01 11:40:00',
        NULL
    );

INSERT INTO chat_channel_audit_log (
    audit_id,
    channel_id,
    actor_account_id,
    action_type,
    target_account_id,
    metadata,
    created_at
)
VALUES
    (
        7001,
        100,
        1001,
        'MEMBER_PROMOTED_TO_ADMIN',
        1002,
        '{\"from\":\"MEMBER\",\"to\":\"ADMIN\"}',
        '2026-05-01 11:41:00'
    ),
    (
        7002,
        101,
        1001,
        'MEMBER_BANNED',
        1003,
        '{\"reason\":\"spam links\"}',
        '2026-05-01 11:42:00'
    );

INSERT INTO chat_message (
    message_id,
    sender_id,
    channel_id,
    domain,
    domain_version,
    data,
    send_time,
    mentions,
    preview,
    status
)
VALUES
    (
        5001,
        1001,
        1,
        'Core:Text',
        '1.0.0',
        '{\"text\":\"Hello public channel\"}',
        '2026-05-01 12:00:00',
        '[]',
        'Hello public channel',
        'sent'
    ),
    (
        5002,
        1001,
        2,
        'Core:System',
        '1.0.0',
        '{\"text\":\"System channel bootstrap completed.\",\"event\":\"bootstrap_completed\",\"level\":\"info\"}',
        '2026-05-01 12:01:00',
        '[]',
        'System channel bootstrap completed.',
        'sent'
    ),
    (
        5003,
        1001,
        100,
        'Core:Text',
        '1.0.0',
        '{\"text\":\"@carry-admin Please review the API draft.\"}',
        '2026-05-01 12:02:00',
        '[\"1002\"]',
        '@carry-admin Please review the API draft.',
        'sent'
    ),
    (
        5004,
        1002,
        100,
        'Core:ReplyText',
        '1.0.0',
        '{\"content\":{\"text\":\"Updated checklist is ready.\"},\"reply_to_mid\":\"5003\",\"reply_to\":{\"mid\":\"5003\",\"sender_name\":\"Carry Owner\",\"preview\":\"@carry-admin Please review the API draft.\",\"created_at\":1777608120000,\"unavailable\":false}}',
        '2026-05-01 12:03:00',
        '[]',
        'Updated checklist is ready.',
        'sent'
    ),
    (
        5005,
        1002,
        101,
        'Core:Forward',
        '1.0.0',
        '{\"domain\":\"Core:Text\",\"domain_version\":\"1.0.0\",\"content\":{\"text\":\"Forwarded summary from public channel.\"},\"forwarded_from\":{\"mid\":\"5001\",\"cid\":\"1\",\"uid\":\"1001\",\"preview\":\"Hello public channel\",\"send_time\":1777608000000}}',
        '2026-05-01 12:06:00',
        '[]',
        'Forwarded summary from public channel.',
        'sent'
    );

INSERT INTO chat_channel_read_state (
    channel_id,
    account_id,
    last_read_message_id,
    last_read_time,
    created_at,
    updated_at
)
VALUES
    (1, 1001, 5001, '2026-05-01 12:00:30', '2026-05-01 12:00:30', '2026-05-01 12:00:30'),
    (1, 1003, 5001, '2026-05-01 12:01:30', '2026-05-01 12:01:30', '2026-05-01 12:01:30'),
    (100, 1001, 5004, '2026-05-01 12:05:30', '2026-05-01 12:05:30', '2026-05-01 12:05:30'),
    (100, 1002, 5004, '2026-05-01 12:05:45', '2026-05-01 12:05:45', '2026-05-01 12:05:45'),
    (101, 1002, 5005, '2026-05-01 12:06:30', '2026-05-01 12:06:30', '2026-05-01 12:06:30');

INSERT INTO chat_channel_pin (
    pin_id,
    channel_id,
    message_id,
    pinned_by_account_id,
    note,
    pinned_at
)
VALUES
    (
        8001,
        100,
        5004,
        1001,
        'iteration focus',
        '2026-05-01 12:07:00'
    );

INSERT INTO chat_mention (
    mention_id,
    channel_id,
    message_id,
    from_account_id,
    target_type,
    target_account_id,
    created_at,
    is_read
)
VALUES
    (
        9001,
        100,
        5003,
        1001,
        'user',
        1002,
        '2026-05-01 12:02:00',
        FALSE
    );

INSERT INTO chat_notification_server_preference (
    account_id,
    mode,
    muted_until,
    created_at,
    updated_at
)
VALUES
    (1001, 'all', 0, '2026-05-01 12:10:00', '2026-05-01 12:10:00'),
    (1002, 'mentions_only', 0, '2026-05-01 12:10:10', '2026-05-01 12:10:10'),
    (1003, 'muted', 1770000000000, '2026-05-01 12:10:20', '2026-05-01 12:10:20');

INSERT INTO chat_notification_channel_preference (
    account_id,
    channel_id,
    mode,
    muted_until,
    created_at,
    updated_at
)
VALUES
    (1001, 100, 'all', 0, '2026-05-01 12:11:00', '2026-05-01 12:11:00'),
    (1002, 100, 'inherit', 0, '2026-05-01 12:11:10', '2026-05-01 12:11:10'),
    (1003, 1, 'muted', 1770000000000, '2026-05-01 12:11:20', '2026-05-01 12:11:20'),
    (1002, 101, 'mentions_only', 0, '2026-05-01 12:11:30', '2026-05-01 12:11:30');

COMMIT;

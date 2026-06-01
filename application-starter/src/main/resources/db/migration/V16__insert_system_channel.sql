INSERT INTO chat_channel (
    id,
    conversation_id,
    name,
    brief,
    avatar,
    type,
    is_default,
    created_at,
    updated_at
)
SELECT
    2,
    2,
    'system',
    '',
    '',
    'system',
    FALSE,
    '2026-04-22 00:00:00',
    '2026-04-22 00:00:00'
WHERE NOT EXISTS (
    SELECT 1
    FROM chat_channel
    WHERE type = 'system'
);

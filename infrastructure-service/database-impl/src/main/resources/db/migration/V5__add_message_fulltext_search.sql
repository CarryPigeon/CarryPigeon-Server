-- 正文由各消息处理器明确派生，不能直接对 JSON 或截断预览建立全文索引。
ALTER TABLE chat_message ADD COLUMN search_text TEXT NOT NULL;

-- 只回填当前已实现的文本协议正文；撤回消息及其它 domain 不从元数据猜测可检索内容。
UPDATE chat_message
SET search_text = CASE
    WHEN domain = 'Core:Text:V1' AND status = 'sent' AND JSON_TYPE(JSON_EXTRACT(data, '$.content')) = 'STRING'
    THEN JSON_UNQUOTE(JSON_EXTRACT(data, '$.content'))
    ELSE ''
END;

-- 使用 MySQL 内置 ngram，不依赖外部检索服务。词元长度与停用词沿用实例设置（默认词元长度为 2）。
ALTER TABLE chat_message ADD FULLTEXT INDEX ft_chat_message_search_text (search_text) WITH PARSER ngram;

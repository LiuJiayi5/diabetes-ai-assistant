-- Module 5 migration: persist Dify conversation id for multi-turn AI doctor chat.
-- Safe to run repeatedly on MySQL 8.0+.

SET @column_exists := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'ai_chat_sessions'
      AND COLUMN_NAME = 'dify_conversation_id'
);

SET @sql := IF(
    @column_exists = 0,
    'ALTER TABLE ai_chat_sessions ADD COLUMN dify_conversation_id VARCHAR(128) NULL COMMENT ''Dify conversation id for multi-turn chat'' AFTER session_title',
    'SELECT ''ai_chat_sessions.dify_conversation_id already exists'' AS message'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @index_exists := (
    SELECT COUNT(*)
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'ai_chat_sessions'
      AND INDEX_NAME = 'idx_dify_conversation'
);

SET @sql := IF(
    @index_exists = 0,
    'ALTER TABLE ai_chat_sessions ADD INDEX idx_dify_conversation (dify_conversation_id)',
    'SELECT ''ai_chat_sessions.idx_dify_conversation already exists'' AS message'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

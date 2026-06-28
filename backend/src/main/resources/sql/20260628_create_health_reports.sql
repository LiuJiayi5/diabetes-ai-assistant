CREATE TABLE IF NOT EXISTS health_reports (
    report_id INT NOT NULL AUTO_INCREMENT,
    user_id INT NOT NULL,
    report_type VARCHAR(32) NOT NULL,
    report_title VARCHAR(120) NOT NULL,
    report_markdown MEDIUMTEXT NOT NULL,
    report_summary TEXT DEFAULT NULL,
    data_snapshot_json MEDIUMTEXT DEFAULT NULL,
    completeness_score INT NOT NULL DEFAULT 0,
    report_status VARCHAR(20) NOT NULL DEFAULT 'generated',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (report_id),
    KEY idx_health_reports_user (user_id),
    KEY idx_health_reports_type (report_type),
    KEY idx_health_reports_created (create_time),
    CONSTRAINT fk_health_reports_user
        FOREIGN KEY (user_id) REFERENCES users(user_id)
        ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='健康管理报告';

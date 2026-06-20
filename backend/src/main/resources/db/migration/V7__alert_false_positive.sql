-- M5+ 误报反馈：告警增加误报标记，供灰度发布误报率计算
ALTER TABLE alert ADD COLUMN false_positive TINYINT NOT NULL DEFAULT 0;

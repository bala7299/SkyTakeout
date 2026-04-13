-- 专门给 AI 评论用的新表，不影响其他表
CREATE TABLE IF NOT EXISTS `order_comment`
(
    `id`            bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
    `order_id`      bigint NOT NULL COMMENT '订单id',
    `user_id`       bigint NOT NULL COMMENT '用户id',
    `score`         int          DEFAULT '5' COMMENT '评分：1-5分',
    `content`       varchar(500) DEFAULT NULL COMMENT '原始评论内容',
    `ai_optimized`  varchar(500) DEFAULT NULL COMMENT 'AI润色后的建议回复',
    `create_time`   datetime     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `reply_content` varchar(500) DEFAULT NULL COMMENT '商家回复内容',
    `status`        int          DEFAULT '0' COMMENT '状态：0未回复，1已回复',
    PRIMARY KEY (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='订单评价表';
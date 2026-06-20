package com.puxun.monitor.alert.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.Instant;

@Data
@TableName("wecom_push_log")
public class WecomPushLog implements Serializable {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String alertIds;
    private Long channelId;
    private String payloadJson;
    private Integer httpStatus;
    private String result;
    private Instant pushedAt;
}

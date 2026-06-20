package com.puxun.monitor.result.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

@Data
@TableName("check_violation_sample")
public class CheckViolationSample implements Serializable {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long checkResultId;
    private String sampleJson;
}

package com.puxun.monitor.alert;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.puxun.monitor.alert.domain.AlertChannel;
import com.puxun.monitor.alert.mapper.AlertChannelMapper;
import com.puxun.monitor.alert.wecom.WecomPushService;
import com.puxun.monitor.audit.Audited;
import com.puxun.monitor.common.BizException;
import com.puxun.monitor.common.ResultCode;
import com.puxun.monitor.datasource.security.CipherService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AlertChannelService {

    private final AlertChannelMapper mapper;
    private final CipherService cipher;
    private final WecomPushService pushService;

    /** webhook/配置以明文入参，加密存储；响应不回显。 */
    public record SaveCmd(Long id, String name, String type, String config, String msgType,
                          String mentionMobiles, String gateSeverity, Integer dedupWindowSec,
                          String scopeType, String scopeRef, Integer enabled) {}

    public List<AlertChannel> list() {
        List<AlertChannel> list = mapper.selectList(Wrappers.<AlertChannel>lambdaQuery());
        list.forEach(c -> c.setConfigCipher(null)); // 不回显
        return list;
    }

    @Audited(action = "EDIT_ALERT_CHANNEL")
    public Long save(SaveCmd cmd) {
        AlertChannel c = cmd.id() != null ? mapper.selectById(cmd.id()) : new AlertChannel();
        if (c == null) throw new BizException(ResultCode.NOT_FOUND, "渠道不存在");
        c.setName(cmd.name());
        c.setType(cmd.type() != null ? cmd.type() : "WEBHOOK");
        if (cmd.config() != null && !cmd.config().isBlank()) {
            c.setConfigCipher(cipher.encrypt(cmd.config()));
        }
        c.setMsgType(cmd.msgType() != null ? cmd.msgType() : "MARKDOWN");
        c.setMentionMobiles(cmd.mentionMobiles());
        c.setGateSeverity(cmd.gateSeverity() != null ? cmd.gateSeverity() : "HIGH");
        c.setDedupWindowSec(cmd.dedupWindowSec() != null ? cmd.dedupWindowSec() : 300);
        c.setScopeType(cmd.scopeType() != null ? cmd.scopeType() : "DEFAULT");
        c.setScopeRef(cmd.scopeRef());
        c.setEnabled(cmd.enabled() != null ? cmd.enabled() : 1);
        if (cmd.id() != null) mapper.updateById(c); else mapper.insert(c);
        return c.getId();
    }

    public WecomPushService.PushResult test(Long id) {
        AlertChannel c = mapper.selectById(id);
        if (c == null) throw new BizException(ResultCode.NOT_FOUND, "渠道不存在");
        return pushService.test(c);
    }
}

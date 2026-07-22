package io.contentpublisher.platform.web.form;

import io.contentpublisher.platform.domain.ChannelType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class CreateChannelAccountForm {
    @NotNull(message = "请选择渠道类型")
    private ChannelType type;

    @NotBlank(message = "账号名称不能为空")
    @Size(max = 120, message = "账号名称不能超过 120 个字符")
    private String displayName;

    @Size(max = 2048, message = "站点地址不能超过 2048 个字符")
    private String baseUrl;

    @Size(max = 4000, message = "凭据内容过长")
    private String credentialOne;

    @Size(max = 4000, message = "凭据内容过长")
    private String credentialTwo;

    @Size(max = 4000, message = "凭据内容过长")
    private String credentialThree;

    @Size(max = 4000, message = "凭据内容过长")
    private String credentialFour;

    @Size(max = 4000, message = "凭据内容过长")
    private String credentialFive;

    @NotBlank
    @Pattern(regexp = "[A-Za-z0-9._:-]{8,128}")
    private String idempotencyKey;

    public ChannelType getType() { return type; }
    public void setType(ChannelType type) { this.type = type; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
    public String getCredentialOne() { return credentialOne; }
    public void setCredentialOne(String credentialOne) { this.credentialOne = credentialOne; }
    public String getCredentialTwo() { return credentialTwo; }
    public void setCredentialTwo(String credentialTwo) { this.credentialTwo = credentialTwo; }
    public String getCredentialThree() { return credentialThree; }
    public void setCredentialThree(String credentialThree) { this.credentialThree = credentialThree; }
    public String getCredentialFour() { return credentialFour; }
    public void setCredentialFour(String credentialFour) { this.credentialFour = credentialFour; }
    public String getCredentialFive() { return credentialFive; }
    public void setCredentialFive(String credentialFive) { this.credentialFive = credentialFive; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }
}

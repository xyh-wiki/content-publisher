package io.contentpublisher.platform.web.form;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class RejectArticleForm {
    @NotBlank(message = "驳回原因不能为空")
    @Size(max = 500, message = "驳回原因不能超过 500 个字符")
    private String reason;

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}

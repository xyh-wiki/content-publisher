package io.contentpublisher.platform.web.form;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class ImportProjectForm {
    @NotBlank(message = "Git 地址不能为空")
    @Size(max = 2048, message = "Git 地址不能超过 2048 个字符")
    private String gitUrl;

    @Size(max = 200, message = "分支名称不能超过 200 个字符")
    private String branch;

    @NotBlank
    @Pattern(regexp = "[A-Za-z0-9._:-]{8,128}")
    private String idempotencyKey;

    public String getGitUrl() { return gitUrl; }
    public void setGitUrl(String gitUrl) { this.gitUrl = gitUrl; }
    public String getBranch() { return branch; }
    public void setBranch(String branch) { this.branch = branch; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }
}

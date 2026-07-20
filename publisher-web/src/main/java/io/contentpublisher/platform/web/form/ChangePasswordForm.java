package io.contentpublisher.platform.web.form;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ChangePasswordForm {
    @NotBlank(message = "当前密码不能为空")
    @Size(max = 200)
    private String currentPassword;

    @NotBlank(message = "新密码不能为空")
    @Size(min = 8, max = 128, message = "新密码必须为 8 到 128 个字符")
    private String newPassword;

    @NotBlank(message = "确认密码不能为空")
    @Size(min = 8, max = 128)
    private String confirmPassword;

    public String getCurrentPassword() { return currentPassword; }
    public void setCurrentPassword(String currentPassword) { this.currentPassword = currentPassword; }
    public String getNewPassword() { return newPassword; }
    public void setNewPassword(String newPassword) { this.newPassword = newPassword; }
    public String getConfirmPassword() { return confirmPassword; }
    public void setConfirmPassword(String confirmPassword) { this.confirmPassword = confirmPassword; }
}

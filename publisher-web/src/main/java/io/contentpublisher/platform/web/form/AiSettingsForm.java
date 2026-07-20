package io.contentpublisher.platform.web.form;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class AiSettingsForm {
    @Min(0)
    private int expectedVersion;
    private boolean enabled;

    @NotBlank(message = "AI Base URL 不能为空")
    @Size(max = 2048, message = "AI Base URL 不能超过 2048 个字符")
    private String baseUrl;

    @Size(max = 8000, message = "API Key 不能超过 8000 个字符")
    private String apiKey;
    private boolean clearApiKey;

    @NotBlank(message = "模型名称不能为空")
    @Size(max = 200, message = "模型名称不能超过 200 个字符")
    private String model;

    @Min(value = 5, message = "超时不能少于 5 秒")
    @Max(value = 300, message = "超时不能超过 300 秒")
    private int timeoutSeconds;

    @DecimalMin(value = "0.0", message = "温度不能小于 0")
    @DecimalMax(value = "1.0", message = "温度不能大于 1")
    private double temperature;

    public int getExpectedVersion() { return expectedVersion; }
    public void setExpectedVersion(int expectedVersion) { this.expectedVersion = expectedVersion; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }
    public boolean isClearApiKey() { return clearApiKey; }
    public void setClearApiKey(boolean clearApiKey) { this.clearApiKey = clearApiKey; }
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public int getTimeoutSeconds() { return timeoutSeconds; }
    public void setTimeoutSeconds(int timeoutSeconds) { this.timeoutSeconds = timeoutSeconds; }
    public double getTemperature() { return temperature; }
    public void setTemperature(double temperature) { this.temperature = temperature; }
}

package io.contentpublisher.platform.application;

public record ChannelConnectionResult(boolean succeeded, String message) {
    public ChannelConnectionResult {
        if (message == null || message.isBlank()) message = succeeded ? "连接验证成功" : "连接验证失败";
        message = message.length() <= 500 ? message : message.substring(0, 500);
    }

    public static ChannelConnectionResult success(String message) {
        return new ChannelConnectionResult(true, message);
    }

    public static ChannelConnectionResult failure(String message) {
        return new ChannelConnectionResult(false, message);
    }
}

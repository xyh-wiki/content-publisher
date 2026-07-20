package io.contentpublisher.platform.web.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.Map;

public record RotateChannelCredentialsRequest(
        @Min(1) int expectedVersion,
        @NotEmpty @Size(max = 10) Map<@NotBlank @Size(max = 100) String,
                @NotBlank @Size(max = 4000) String> credentials) {
}

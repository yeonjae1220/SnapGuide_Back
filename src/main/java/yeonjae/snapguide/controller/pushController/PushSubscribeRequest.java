package yeonjae.snapguide.controller.pushController;

import jakarta.validation.constraints.NotBlank;

public record PushSubscribeRequest(
        @NotBlank String endpoint,
        Keys keys
) {
    public record Keys(@NotBlank String auth, @NotBlank String p256dh) {}
}

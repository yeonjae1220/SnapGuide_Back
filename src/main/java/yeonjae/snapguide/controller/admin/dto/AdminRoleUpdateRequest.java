package yeonjae.snapguide.controller.admin.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import yeonjae.snapguide.domain.member.Authority;

import java.util.List;

@Getter
@NoArgsConstructor
public class AdminRoleUpdateRequest {
    @NotNull
    private List<Authority> authority;
}

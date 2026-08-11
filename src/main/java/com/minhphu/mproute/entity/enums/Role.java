package com.minhphu.mproute.entity.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Vai tro nguoi dung. Spring Security can tien to ROLE_ khi tao GrantedAuthority,
 * nen gia tri ADMIN o day tuong ung voi hasRole("ADMIN") trong SecurityConfig.
 */
@Getter
@RequiredArgsConstructor
public enum Role {

    ADMIN("Quản trị viên"),
    DISPATCHER("Điều phối viên"),
    DRIVER("Tài xế");

    private final String displayName;
}

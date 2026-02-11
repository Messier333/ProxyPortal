package com.messier333.proxyportal.dashboard.dto;

import com.messier333.proxyportal.user.entity.Role;

public record UserDashboardVM(
        Long id,
        String username,
        Role role
) {
}

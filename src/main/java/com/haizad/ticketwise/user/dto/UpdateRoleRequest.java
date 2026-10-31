package com.haizad.ticketwise.user.dto;

import com.haizad.ticketwise.user.UserRole;
import jakarta.validation.constraints.NotNull;

public record UpdateRoleRequest(@NotNull UserRole role) {
}

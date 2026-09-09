package com.achadosedevolvidos.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record RefreshRequest(

        @NotBlank(message = "refreshToken é obrigatório")
        String refreshToken
) {}

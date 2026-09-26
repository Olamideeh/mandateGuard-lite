package com.example.mandateguard.dto;

import jakarta.validation.constraints.Size;

public record ApprovalDecisionRequest(

        @Size(
                max = 1000,
                message = "Decision note cannot exceed 1000 characters"
        )
        String decisionNote
) {
}
package com.codevision.codevisionbackend.analyze;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AnalyzeRequest {

    @NotBlank
    private String repoUrl;
}

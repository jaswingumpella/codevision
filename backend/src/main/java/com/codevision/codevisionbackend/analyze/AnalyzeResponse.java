package com.codevision.codevisionbackend.analyze;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AnalyzeResponse {

    private Long projectId;
    private String status;
}

package com.rag.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class HandoffRequest {

    @NotBlank(message = "请选择知识库")
    private String kbId;

    @Size(max = 500)
    private String question;

    @Size(max = 200)
    private String remark;
}

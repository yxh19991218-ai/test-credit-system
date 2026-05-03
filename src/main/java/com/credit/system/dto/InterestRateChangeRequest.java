package com.credit.system.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class InterestRateChangeRequest {

    @NotNull(message = "新利率不能为空")
    @DecimalMin(value = "0.06", message = "利率不能低于6%")
    @DecimalMax(value = "0.24", message = "利率不能高于24%")
    private BigDecimal newRate;

    @NotBlank(message = "变更类型不能为空")
    private String changeType;

    @NotBlank(message = "变更原因不能为空")
    @Size(max = 500, message = "变更原因不能超过500个字符")
    private String reason;
}

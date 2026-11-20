package com.admintem.web.entity.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/** MFA 状态。 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "MFA 状态")
public class MfaStatusVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "是否已开启 MFA")
    private Boolean enabled;
}

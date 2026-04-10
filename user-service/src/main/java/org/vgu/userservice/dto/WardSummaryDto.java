package org.vgu.userservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WardSummaryDto {
    private String hospitalId;
    private String wardId;
    private long doctorCount;
    private long nurseCount;
}

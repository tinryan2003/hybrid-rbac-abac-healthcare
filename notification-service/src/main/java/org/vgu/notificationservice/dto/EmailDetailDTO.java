package org.vgu.notificationservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmailDetailDTO {
    private String to;
    private String subject;
    private String templateName;
    private Map<String, Object> dynamicValue;
}


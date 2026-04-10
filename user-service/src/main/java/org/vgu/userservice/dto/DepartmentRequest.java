package org.vgu.userservice.dto;

import lombok.Data;

@Data
public class DepartmentRequest {
    private String name;
    private String location;
    private String hospitalId;
    private String description;
}


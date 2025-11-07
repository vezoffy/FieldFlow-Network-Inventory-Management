package com.deploymentservice.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TechnicianCreationRequest {
    private String name;
    private String contact;
    private String region;
    private String username;
    private String password;
}

package com.apnastorage.users.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SignupRequest {

    private String name;
    private String username;
    private String email;
    private String mob;
    private String password;
}

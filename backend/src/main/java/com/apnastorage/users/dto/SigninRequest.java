package com.apnastorage.users.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SigninRequest {

    private String username;
    private String password;
}

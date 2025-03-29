package com.sss.backend.api.dto.OAuth;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserDTO {
    private String role;
    private String name;
    private String username;

    public UserDTO(String username, String role) {
        this.username = username;
        this.role = role;
    }
}

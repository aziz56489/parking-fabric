package com.parking.service;

import org.springframework.stereotype.Service;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@Service 
public class PasswordEncoderService {
    private final BCryptPasswordEncoder bcrypt = new BCryptPasswordEncoder();

    public String encode(String rawPassword){
        if(rawPassword == null){
            return null;
        }
        return bcrypt.encode(rawPassword);
    } 
    public boolean matches(String rawPassword, String encodedPassword){
        if (rawPassword == null || encodedPassword == null){
            return false;
        }
        if(encodedPassword.startsWith("$2a$") || encodedPassword.startsWith("$2b$") || encodedPassword.startsWith("$2y$")){
            return bcrypt.matches(rawPassword, encodedPassword);
        }
        return rawPassword.equals(encodedPassword);
    }
}

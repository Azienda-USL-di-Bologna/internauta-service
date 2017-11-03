package it.bologna.ausl.baborg.security.jwt;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtUtils {
    
    @Value("${jwt.secret}")
    private static String SECRET_KEY;

    public static String getSECRET_KEY() {
        return SECRET_KEY;
    }

    
    
}

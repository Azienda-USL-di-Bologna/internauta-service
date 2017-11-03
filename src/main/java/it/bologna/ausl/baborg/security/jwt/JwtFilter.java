package it.bologna.ausl.baborg.security.jwt;

import java.io.IOException;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import org.springframework.web.filter.GenericFilterBean;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;


public class  JwtFilter extends GenericFilterBean {

    
    private String SECRET_KEY;
    
    JwtFilter(String secretKey){
        super();
        this.SECRET_KEY=secretKey;
    }
    
    @Override
    public void doFilter(final ServletRequest req,
            final ServletResponse res,
            final FilterChain chain) throws IOException, ServletException {
        
        final HttpServletRequest request = (HttpServletRequest) req;

        final String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new ServletException("Missing or invalid Authorization header.");
        }

        final String token = authHeader.substring(7); // la parte dopo "Bearer "

        try {
            final Claims claims = Jwts.parser().setSigningKey(SECRET_KEY)
                    .parseClaimsJws(token).getBody();
            
            request.setAttribute("claims", claims);
        } catch (final SignatureException e) {
            throw new ServletException("Invalid token.");
        }

        chain.doFilter(req, res);
    }
}

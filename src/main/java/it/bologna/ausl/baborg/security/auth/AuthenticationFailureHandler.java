package it.bologna.ausl.baborg.security.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.bologna.ausl.baborg.security.login.bean.LoginResponseDTO;
import it.bologna.ausl.baborg.security.login.bean.LoginStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Created by f.longhitano on 14/07/2017.
 */

@Component
public class AuthenticationFailureHandler extends SimpleUrlAuthenticationFailureHandler {


    @Autowired
    ObjectMapper objectMapper;

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                        AuthenticationException exception) throws IOException, ServletException {

        //super.onAuthenticationFailure(request, response, exception);
        LoginResponseDTO loginResponseDTO=new LoginResponseDTO();

        loginResponseDTO.setMessageStatus(LoginStatus.LOGIN_ERROR);
        String jwtResponse = objectMapper.writeValueAsString( loginResponseDTO );
        response.setContentType("application/json");
        response.getWriter().write( jwtResponse );
    }
}
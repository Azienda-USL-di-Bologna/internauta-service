package it.bologna.ausl.baborg.security.auth;


import com.fasterxml.jackson.databind.ObjectMapper;
import it.bologna.ausl.baborg.entities.Utente;
import it.bologna.ausl.baborg.security.UserTokenState;
import it.bologna.ausl.baborg.security.login.bean.LoginResponseDTO;
import it.bologna.ausl.baborg.security.login.bean.LoginStatus;
import it.bologna.ausl.baborg.security.utils.TokenHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;


/**
 * Handler di login che gestisce il caso di login con successo
 */
@Component
public class AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    @Value("${jwt.expires_in}")
    private int EXPIRES_IN;

    @Value("${jwt.cookie}")
    private String TOKEN_COOKIE;

	@Autowired
	TokenHelper tokenHelper;

	@Autowired
    ObjectMapper objectMapper;


	/**
	 * Scrive nella response un bean di tipo {@link LoginResponseDTO} per comunicare l'esito positivo del login
	 *
	 * @param request
	 * @param response
	 * @param authentication
	 * @throws IOException
	 * @throws ServletException
	 */
	@Override
	public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication ) throws IOException, ServletException {
		clearAuthenticationAttributes(request);
		Utente user = (Utente) authentication.getPrincipal();

		String jws = tokenHelper.generateToken( user.getUsername() );

        // Create token auth Cookie
        Cookie authCookie = new Cookie( TOKEN_COOKIE, ( jws ) );
		authCookie.setPath( "/" );
		authCookie.setHttpOnly( true );
		authCookie.setMaxAge( EXPIRES_IN );
		// Add cookie to response
		response.addCookie( authCookie );
		// JWT is also in the response
		LoginResponseDTO loginResponseDTO=new LoginResponseDTO();
		loginResponseDTO.setUserTokenState(new UserTokenState(jws, EXPIRES_IN));
		if(authentication.getPrincipal()!=null && UserDetails.class.isAssignableFrom(authentication.getPrincipal().getClass()))
			loginResponseDTO.setUserDetails((UserDetails) authentication.getPrincipal());
		loginResponseDTO.setMessageStatus(LoginStatus.LOGIN_OK);
		UserTokenState userTokenState = new UserTokenState(jws, EXPIRES_IN);
		String jwtResponse = objectMapper.writeValueAsString( loginResponseDTO );
		response.setContentType("application/json");
		response.getWriter().write( jwtResponse );

	}
}

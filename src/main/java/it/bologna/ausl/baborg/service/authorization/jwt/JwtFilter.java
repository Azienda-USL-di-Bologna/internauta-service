package it.bologna.ausl.baborg.service.authorization.jwt;

/**
 *
 * @author spritz
 */
import io.jsonwebtoken.Claims;
import java.io.IOException;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import org.springframework.web.filter.GenericFilterBean;

public class JwtFilter extends GenericFilterBean {

    private final String secretKey;
    private final AuthorizationUtils authorizationUtils;

    public JwtFilter(String secretKey, AuthorizationUtils authorizationUtils) {
        super();
        this.secretKey = secretKey;
        this.authorizationUtils = authorizationUtils;
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

        // la parte dopo "Bearer "
        final String token = authHeader.substring(7);

        try {
            Claims claims = authorizationUtils.setInSecurityContext(token, secretKey);
            request.setAttribute("claims", claims);
        } catch (ClassNotFoundException ex) {
            throw new ServletException("Invalid token", ex);
        }

        chain.doFilter(req, res);
    }
}

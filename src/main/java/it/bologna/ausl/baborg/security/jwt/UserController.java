package it.bologna.ausl.baborg.security.jwt;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import it.bologna.ausl.baborg.utils.PasswordHash;
import it.bologna.ausl.entities.baborg.Utente;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Date;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.cxf.common.util.ReflectionInvokationHandler;
import org.apache.http.HttpResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/user")
public class UserController {

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);
    // dati fake prim di essere inserire i dati da DB
    //private final Map<String, List<String>> userDb = new HashMap<>();
    private SignatureAlgorithm SIGNATURE_ALGORITHM = SignatureAlgorithm.HS256;

    @Value("${jwt.secret}")
    private String SECRET_KEY;

    @Value("${saml.user.field:CodiceFiscale}")
    private String samlUser;

    @Value("${saml.db.login_field:cf}")
    private String dbField;

    @Autowired
    CustomUserDetailsService userDb;

    public UserController() {
    }

    @RequestMapping(value = "login", method = RequestMethod.POST)
    public LoginResponse loginPOST(@RequestBody final UserLogin userLogin) throws ServletException, NoSuchAlgorithmException, InvalidKeySpecException {
        UserDetails ud = null;

        logger.debug("login username: " + userLogin.username);

        // considera username
        ud = userDb.loadUserByUsername(userLogin.username);
        if (userLogin.username == null || ud == null) {
            throw new ServletException("Invalid login");
        }

        // considera password
        if (userLogin.password == null || ud == null) {
            throw new ServletException("Invalid login");
        }

        if (!PasswordHash.validatePassword(userLogin.password, ud.getPassword())) {
            throw new ServletException("Invalid login");
        }

        logger.info(String.format("User: %s logged in %s ", ud.getUsername(), ((Utente) ud).getDescrizione()));
        return new LoginResponse(Jwts.builder().setSubject(ud.getUsername())
                .claim("roles", "admin").setIssuedAt(new Date())
                .signWith(SIGNATURE_ALGORITHM, SECRET_KEY).compact(),
                ud.getUsername());
    }

    @RequestMapping(value = "login", method = RequestMethod.GET)
    public LoginResponse loginGET(HttpServletRequest request) throws ServletException, NoSuchAlgorithmException, InvalidKeySpecException, IOException {
        UserDetails ud = null;
        //LOGIN SAML
        String user = request.getAttribute(samlUser).toString();
        ud = userDb.loadByParameter(dbField, user);
        if (ud == null) {
            throw new ServletException("User not found");
        }
        logger.info(String.format("User: %s logged in %s ", ud.getUsername(), ((Utente) ud).getDescrizione()));
        return new LoginResponse(Jwts.builder().setSubject(ud.getUsername())
                .claim("roles", "admin").setIssuedAt(new Date())
                .signWith(SIGNATURE_ALGORITHM, SECRET_KEY).compact(),
                ud.getUsername());
    }

    @SuppressWarnings("unused")
    public static class UserLogin {

        public String username;
        public String password;
    }

    @SuppressWarnings("unused")
    public static class LoginResponse {

        public String token;
        public String username;

        public LoginResponse(final String token, final String username) {
            this.token = token;
            this.username = username;
        }
    }
}

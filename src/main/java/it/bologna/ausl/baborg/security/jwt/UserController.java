package it.bologna.ausl.baborg.security.jwt;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import it.bologna.ausl.baborg.utils.PasswordHash;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Date;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
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
    
    public UserController() {}
        
//        userDb.put("tom", Arrays.asList("user"));
//        userDb.put("sally", Arrays.asList("user", "admin"));
//    }

      @RequestMapping(value = "login", method = RequestMethod.POST)
    public LoginResponse login(@RequestBody final UserLogin userLogin, HttpServletRequest request) throws ServletException, NoSuchAlgorithmException, InvalidKeySpecException {
        UserDetails ud = null;
        //LOGIN SAML
        if (StringUtils.hasText(samlUser)) {
            String user = request.getAttribute(samlUser).toString();
            ud = userDb.loadByParameter(dbField, user);
            if (ud==null){
             throw new ServletException("User not found");
            }
        } else {
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
        }
//        if (login.username == null || !userDb.containsKey(login.username)) {
//            throw new ServletException("Invalid login");
//        }

        return new LoginResponse(Jwts.builder().setSubject(userLogin.username)
                .claim("roles", "admin").setIssuedAt(new Date())
                .signWith(SIGNATURE_ALGORITHM, SECRET_KEY).compact(),
                userLogin.username);
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
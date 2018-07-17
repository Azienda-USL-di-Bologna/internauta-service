//package it.bologna.ausl.shalbo.authorization.jwt;
//
//import it.bologna.ausl.shalbo.entities.Azienda;
//import java.util.Collection;
//import org.springframework.security.authentication.AbstractAuthenticationToken;
//import org.springframework.security.core.GrantedAuthority;
//
///**
// *
// * @author spritz
// */
//public class TokenBasedAuthentication extends AbstractAuthenticationToken {
//
//    private String token;
//    private Azienda azienda;
////    private final UserDetails userInfo;
//
////    public TokenBasedAuthentication(Test userInfo) {
////        super(null);
////        this.userInfo = userInfo;
//////        super.setDetails(userInfo);
////    }
//    
//    public TokenBasedAuthentication(Collection<? extends GrantedAuthority> authorities) {
//        super(authorities);
//    }
//
//    @Override
//    public Object getCredentials() {
//        return token;
//    }
//
//    @Override
//    public Object getPrincipal() {
//        return null;
//    }
//
//    public String getToken() {
//        return token;
//    }
//
//    public void setToken(String token) {
//        this.token = token;
//    }
//
//    public Azienda getAzienda() {
//        return azienda;
//    }
//
//    public void setAzienda(Azienda azienda) {
//        this.azienda = azienda;
//    }
//    
//    @Override
//    public boolean isAuthenticated() {
//        return true;
//    }
////
////    @Override
////    public UserDetails getPrincipal() {
////        return userInfo;
////    }
//}

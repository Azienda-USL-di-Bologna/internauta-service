//package it.bologna.ausl.shalbo.authorization.jwt;
//
//import it.bologna.ausl.shalbo.authorization.jwt.exceptions.ShalboAuthenticationException;
//import it.bologna.ausl.shalbo.entities.Azienda;
//import it.bologna.ausl.shalbo.utils.CacheableFunctions;
//import java.io.IOException;
//import java.security.cert.CertificateException;
//import org.jose4j.jwt.JwtClaims;
//import org.jose4j.jwt.consumer.InvalidJwtException;
//import org.jose4j.jwt.consumer.JwtConsumer;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.security.core.context.SecurityContextHolder;
//import org.springframework.stereotype.Component;
//import org.springframework.transaction.annotation.Transactional;
//
///**
// *
// * @author spritz
// */
//@Component
//public class AuthorizationUtils {
//
//    @Autowired
//    CacheableFunctions cacheableFunctions;
//
//    @Value("${shalbo.mode}")
//    private String mode;
//
//    public static final String CLAIM_CODICE_REGIONE_AZIENDA = "codiceRegioneAzienda";
//    public static final String CLAIM_MODE = "mode";
//
//    @Transactional
//    public JwtClaims setInSecurityContext(String token, JwtConsumer jwtConsumer) throws CertificateException, InvalidJwtException, IOException, ShalboAuthenticationException {
//
//        JwtClaims jwtClaims = jwtConsumer.processToClaims(token);
//
//        if (jwtClaims.getClaimValue("codiceRegioneAzienda") == null) {
//            throw new ShalboAuthenticationException("codiceRegioneAzienda non trovato");
//        }
//
//        String codiceRegioneAzienda = (String) jwtClaims.getClaimValue(CLAIM_CODICE_REGIONE_AZIENDA);
//        String requestMode = (String) jwtClaims.getClaimValue(CLAIM_MODE);
//
//        if (!requestMode.equalsIgnoreCase(this.mode)) {
//            throw new ShalboAuthenticationException(String.format("%s non può pubblicare su %s", requestMode, this.mode));
//        } else {
//        }
//
//        Azienda azienda = cacheableFunctions.getAziendaByCodiceRegioneAzienda((String) codiceRegioneAzienda);
//
//        if (azienda == null) {
//            throw new ShalboAuthenticationException(String.format("azienda con codice %s non trovata", codiceRegioneAzienda));
//        }
//
//        TokenBasedAuthentication authentication = new TokenBasedAuthentication(null);
//        authentication.setToken(token);
//        authentication.setAzienda(azienda);
//        SecurityContextHolder.getContext().setAuthentication(authentication);
//        return jwtClaims;
//    }
//}

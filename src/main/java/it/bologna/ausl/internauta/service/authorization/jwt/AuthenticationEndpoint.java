package it.bologna.ausl.internauta.service.authorization.jwt;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.bologna.ausl.blackbox.exceptions.BlackBoxPermissionException;
import it.bologna.ausl.internauta.service.authorization.UserInfoService;
import it.bologna.ausl.internauta.service.exceptions.ObjectNotFoundException;
import it.bologna.ausl.internauta.service.exceptions.SSOException;
import it.bologna.ausl.internauta.service.utils.CachedEntities;
import it.bologna.ausl.model.entities.baborg.Azienda;
import it.nextsw.common.utils.CommonUtils;
import java.io.IOException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import javax.servlet.ServletException;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.MalformedClaimException;
import org.jose4j.jwt.consumer.InvalidJwtException;
import org.jose4j.jwt.consumer.JwtConsumer;
import org.jose4j.jwt.consumer.JwtConsumerBuilder;
import org.jose4j.keys.resolvers.X509VerificationKeyResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 *
 * @author guido
 */
@RestController
public class AuthenticationEndpoint {

    private static final Logger logger = LoggerFactory.getLogger(AuthenticationEndpoint.class);

    @Value("${jwt.secret}")
    private String secretKey;

    @Value("${internauta.mode}")
    private String mode;

    @Value("classpath:BABEL_TEST.crt")
    private Resource fileResourceTest;

    @Value("classpath:BABEL_PROD.crt")
    private Resource fileResourceProd;

    @Value("classpath:INTERNAUTA_ACCESS_TEST.crt")
    private Resource internautaAccessTestPK;

    @Autowired
    UserInfoService userInfoService;

    @Autowired
    AuthorizationUtils authorizationUtils;

    @Autowired
    CachedEntities cachedEntities;

    @Autowired
    CommonUtils commonUtils;

    @Autowired
    ObjectMapper objectMapper;

    @RequestMapping(value = "${security.login.endpoint.path}", method = RequestMethod.POST)
    public ResponseEntity<LoginController.LoginResponse> loginInterApplication(@RequestBody final EndpointObject endpointObject, javax.servlet.http.HttpServletRequest request) throws ServletException, CertificateException, IOException, InvalidJwtException, MalformedClaimException, ClassNotFoundException, ObjectNotFoundException, BlackBoxPermissionException, SSOException {

        X509Certificate cert;

        if (endpointObject == null) {
            throw new ServletException("Invalid endpointObject");
        }

        if (endpointObject.jws == null || endpointObject.applicazione == null) {
            throw new ServletException("Invalid jwt");
        }

//        String headerBase64 = endpointObject.jws.split("[.]")[0];
//
//        if ("".equals(headerBase64) || headerBase64 == null) {
//            throw new MalformedJwtException("token malformato");
//        }
//
//        Base64 decoder = new Base64(true);
//        byte[] decodedBytes = decoder.decode(headerBase64);
//
//        String header = new String(decodedBytes);
//
//        Map<String, Object> headerMap = objectMapper.readValue(header, Map.class);
//        if (headerMap.containsKey("kid") && headerMap.get("kid").equals("INTERNAUTA TEST")) {
//        }
//       
        CertificateFactory fact = CertificateFactory.getInstance("X.509");

        if (mode.equalsIgnoreCase("test")) {
            cert = (X509Certificate) fact.generateCertificate(fileResourceTest.getInputStream());
        } else if (mode.equalsIgnoreCase("prod")) {
            cert = (X509Certificate) fact.generateCertificate(fileResourceProd.getInputStream());
        } else {
            throw new ServletException("invalid mode for create cert");
        }

        X509VerificationKeyResolver x509VerificationKeyResolver = new X509VerificationKeyResolver(cert);

        x509VerificationKeyResolver.setTryAllOnNoThumbHeader(true);
        JwtConsumer jwtConsumer = new JwtConsumerBuilder()
                .setRequireSubject() // the JWT must have a subject claim                                
                .setVerificationKeyResolver(x509VerificationKeyResolver)
                .build();

        //  valida il JWT e processa i claims
        JwtClaims jwtClaims = jwtConsumer.processToClaims(endpointObject.jws);
        String impersonatedUser = jwtClaims.getSubject();;
        String realUser = impersonatedUser;
        String idAzienda = null;
        Boolean fromInternetLogin = null;
        if (jwtClaims.hasClaim(AuthorizationUtils.TokenClaims.REAL_USER.toString())
                && StringUtils.hasText(jwtClaims.getStringClaimValue(AuthorizationUtils.TokenClaims.REAL_USER.toString()))) {
            realUser = jwtClaims.getStringClaimValue(AuthorizationUtils.TokenClaims.REAL_USER.toString());
        }
        if (impersonatedUser.equals(realUser)) {
            impersonatedUser = null;
        }
        if (jwtClaims.hasClaim(AuthorizationUtils.TokenClaims.COMPANY.toString())) {
            String codiceAzienda = jwtClaims.getStringClaimValue(AuthorizationUtils.TokenClaims.COMPANY.toString());
            Azienda azienda = cachedEntities.getAziendaFromCodice(codiceAzienda);
            if (azienda != null) {
                idAzienda = azienda.getId().toString();
            }
        }

        logger.info("jwtClaims.hasClaim(AuthorizationUtils.TokenClaims.FROM_INTERNET.toString()): " + jwtClaims.hasClaim(AuthorizationUtils.TokenClaims.FROM_INTERNET.toString()));
        if (jwtClaims.hasClaim(AuthorizationUtils.TokenClaims.FROM_INTERNET.toString())) {
            fromInternetLogin = jwtClaims.getClaimValue(AuthorizationUtils.TokenClaims.FROM_INTERNET.toString(), Boolean.class);
        }

        String hostname = commonUtils.getHostname(request);
        logger.info("fromInternetLogin: " + fromInternetLogin);
        return authorizationUtils.generateResponseEntityFromSAML(idAzienda, hostname, secretKey, request, realUser, impersonatedUser, endpointObject.applicazione, fromInternetLogin, false, false);
    }

    @SuppressWarnings("unused")
    public static class EndpointObject {

        public String jws;
        public String applicazione;
    }
}

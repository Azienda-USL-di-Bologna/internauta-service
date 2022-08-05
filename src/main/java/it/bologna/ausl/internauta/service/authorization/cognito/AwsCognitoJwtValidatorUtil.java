//package it.bologna.ausl.internauta.service.authorization.cognito;
//
//import com.nimbusds.jose.JOSEException;
//import com.nimbusds.jose.JWSAlgorithm;
//import com.nimbusds.jose.jwk.source.JWKSource;
//import com.nimbusds.jose.jwk.source.RemoteJWKSet;
//import com.nimbusds.jose.proc.BadJOSEException;
//import com.nimbusds.jose.proc.JWSKeySelector;
//import com.nimbusds.jose.proc.JWSVerificationKeySelector;
//import com.nimbusds.jwt.JWTClaimsSet;
//import com.nimbusds.jwt.proc.BadJWTException;
//import com.nimbusds.jwt.proc.ConfigurableJWTProcessor;
//import com.nimbusds.jwt.proc.DefaultJWTProcessor;
//import org.springframework.http.HttpStatus;
//
//import java.net.MalformedURLException;
//import java.net.URL;
//import java.text.ParseException;
//import org.apache.http.HttpException;
//
///**
// * @implNote AWS JWT Validator class.
// * @author rkumar2
// */
//public class AwsCognitoJwtValidatorUtil {
//
//    private AwsCognitoJwtValidatorUtil() {
//    }
//
//    /**
//     * This validates the Aws Jwt Token using Nimbus Jose Jwt Library. For
//     * reference please see.
//     *
//     * @see
//     * <a href= "https://docs.aws.amazon.com/cognito/latest/developerguide/amazon-cognito-user-pools-using-tokens-with-identity-providers.html#amazon-cognito-identity-user-pools-using-id-and-access-tokens-in-web-api">
//     * AWS JWT Token</>
//     * @param token
//     * @return JWTClaimsSet
//     */
//    public static JWTClaimsSet validateAWSJwtToken(String token) throws ParseException, JOSEException, BadJOSEException, MalformedURLException {
//
//        /**
//         * AwsCognitoJwtParserUtil class parse the jwt token and gives back the
//         * payload.
//         */
//        String jsonWebKeyFileURL = AwsCognitoJwtParserUtil.getJsonWebKeyURL(token);
//
//        ConfigurableJWTProcessor jwtProcessor = new DefaultJWTProcessor();
//        JWKSource jwkSource = null;
//        jwkSource = new RemoteJWKSet(new URL(jsonWebKeyFileURL));
//        JWSAlgorithm jwsAlgorithm = JWSAlgorithm.RS256;
//        JWSKeySelector keySelector = new JWSVerificationKeySelector(jwsAlgorithm, jwkSource);
//        jwtProcessor.setJWSKeySelector(keySelector);
//        try {
//            JWTClaimsSet claimsSet = jwtProcessor.process(token, null);
//            return claimsSet;
//
//        } catch (BadJWTException e) {
//            throw new HttpException(HttpStatus.UNAUTHORIZED, "TOKEN_EXPIRED " + e.getLocalizedMessage());
//        }
//
//    }
//}

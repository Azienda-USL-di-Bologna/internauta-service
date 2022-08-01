package it.bologna.ausl.internauta.service.authorization.cognito;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class AwsCognitoJwtParserUtils {

    @Autowired
    private ObjectMapper objectMapper;

    public static final String ISS = "iss";
    public static final String JWK_URl_SUFFIX = "/.well-known/jwks.json";
    public static final String JWK_FILE_PREFIX = "/jwk_";
    public static final String JWK_FILE_SUFFIX = ".json";

    public static final String NOT_VALID_JSON_WEB_TOKEN = "Not a valid json web token";
    public static final String TOKEN_EXPIRED = " Aws Jwt Token has expired.";

    private static final int HEADER = 0;
    private static final int PAYLOAD = 1;
    private static final int SIGNATURE = 2;
    private static final int JWT_PARTS = 3;

    public AwsCognitoJwtParserUtils() {
    }

    /**
     * Returns header for a JWT as a JSON object.
     *
     * @param jwt Required valid JSON Web Token as String.
     * @return AWS jwt header as a JsonObject.
     */
    public JsonObject getHeader(String jwt) throws CognitoException {
        try {
            validateJWT(jwt);
            String header = jwt.split("\\.")[HEADER];
            final byte[] headerBytes = Base64.getUrlDecoder().decode(header);
            final String headerString = new String(headerBytes, "UTF-8");
            JsonParser jsonParser = new JsonParser();
            JsonObject jsonObject = (JsonObject) jsonParser.parse(headerString);
            return jsonObject;
        } catch (UnsupportedEncodingException e) {
            throw new CognitoException(HttpStatus.UNAUTHORIZED, NOT_VALID_JSON_WEB_TOKEN, jwt);
        }

    }

    /**
     * Returns payload of a JWT as a JSON object.
     *
     * @param jwt Required valid JSON Web Token as String.
     * @return AWS jwt payload as a JsonObject.
     */
    public Map<String, Object> getPayload(String jwt) throws CognitoException {
        try {
            final String payload = jwt.split("\\.")[PAYLOAD];
            final byte[] payloadBytes = Base64.getUrlDecoder().decode(payload);
            final String payloadString = new String(payloadBytes, "UTF-8");
            Map<String, Object> value = objectMapper.readValue(payloadString, new TypeReference<Map<String, Object>>() {
            });
            return value;
        } catch (UnsupportedEncodingException | JsonProcessingException e) {
            throw new CognitoException(HttpStatus.UNAUTHORIZED, NOT_VALID_JSON_WEB_TOKEN, jwt);
        }
    }

    /**
     * Returns signature of a JWT as a String.
     *
     * @param jwt Required valid JSON Web Token as String.
     * @return AWS JWT signature as a String.
     */
    public String getSignature(String jwt) throws CognitoException {
        try {
//            validateJWT(jwt);
            final String signature = jwt.split("\\.")[SIGNATURE];
            final byte[] signatureBytes = Base64.getUrlDecoder().decode(signature);
            return new String(signatureBytes, "UTF-8");
        } catch (final Exception e) {
            throw new CognitoException(HttpStatus.UNAUTHORIZED, NOT_VALID_JSON_WEB_TOKEN, jwt);
        }
    }

    /**
     * Returns a claim, from the {@code JWT}s' payload, as a String.
     *
     * @param jwt Required valid JSON Web Token as String.
     * @param claim Required claim name as String.
     * @return claim from the JWT as a String.
     */
    public String getClaim(String jwt, String claim) throws CognitoException {
        try {
            final Map<String, Object> payload = getPayload(jwt);
            final Object claimValue = payload.get(claim);

            if (claimValue != null) {
                return claimValue.toString();
            }

        } catch (final Exception e) {
            throw new CognitoException(HttpStatus.UNAUTHORIZED, NOT_VALID_JSON_WEB_TOKEN, jwt);
        }
        return null;
    }

    /**
     * Checks if {@code JWT} is a valid JSON Web Token.
     *
     * @param jwt
     */
    public void validateJWT(String jwt) throws CognitoException {
        // Check if the the JWT has the three parts
        final String[] jwtParts = jwt.split("\\.");
        if (jwtParts.length != JWT_PARTS) {
            throw new CognitoException(HttpStatus.UNAUTHORIZED, NOT_VALID_JSON_WEB_TOKEN, jwt);
        }
    }

    /**
     * Parse the Jwt token and get the token issuer URL including user pool id.
     *
     * @param token
     * @return Json Web Key URL
     * @throws CustomException
     */
//    public String getJsonWebKeyURL(String token) throws CognitoException {
//
//        JsonObject payload = AwsCognitoJwtParserUtils.getPayload(token);
//        JsonElement issJsonElement = payload.get(ISS);
//        if (Objects.isNull(issJsonElement)) {
//            throw new CognitoException(HttpStatus.UNAUTHORIZED, NOT_VALID_JSON_WEB_TOKEN, payload.toString());
//        }
//
//        String issString = issJsonElement.getAsString();
//        String jwkURl = issString + JWK_URl_SUFFIX;
//
//        return jwkURl;
//    }
    /**
     * This method gets the JWK from AWS using token ISS.
     *
     * @param token
     * @return jwk.json Json Web Key file.
     */
//    public File getJsonWebKeyFile(String token) throws CognitoException, IOException {
//
//        JsonObject payload = AwsCognitoJwtParserUtils.getPayload(token);
//        JsonElement issJsonElement = payload.get(ISS);
//        if (Objects.isNull(issJsonElement)) {
//            throw new CognitoException(HttpStatus.UNAUTHORIZED, NOT_VALID_JSON_WEB_TOKEN, payload.toString());
//        }
//
//        String issString = issJsonElement.getAsString();
//        String userPoolName = getUserPoolFromPayload(issString);
//        String jwkURl = issString + JWK_URl_SUFFIX;
//
//        RestTemplate restTemplate = new RestTemplate();
//        ResponseEntity<String> responseEntity = restTemplate.getForEntity(jwkURl, String.class);
//
//        if (APICognitoRestUtils.isHTTPError(responseEntity.getStatusCode())) {
//            throw new CognitoException(responseEntity.getStatusCode(), NOT_VALID_JSON_WEB_TOKEN, responseEntity.getBody());
//
//        } else {
//
//            Path resourceDirectory = Paths.get("src", "main", "resources");
//            File file = new File(resourceDirectory + JWK_FILE_PREFIX + userPoolName + JWK_FILE_SUFFIX);
//
//            if (file.exists()) {
//                return file;
//            }
//            if (responseEntity.getStatusCode().is2xxSuccessful()) {
//                try {
//                    file.createNewFile();
//                    try (Writer writer = new FileWriter(file); BufferedWriter bufferedWriter = new BufferedWriter(writer)) {
//                        bufferedWriter.write(responseEntity.getBody());
//                    }
//
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//
//            }
//
//            return file;
//        }
//
//    }
    /**
     * Get the user pool from the iss url.
     *
     * @param issUrl
     * @return ISS - token issuer URL.
     */
    private String getUserPoolFromPayload(String issUrl) {

        String[] issArray = issUrl.split("amazonaws.com/");
        return issArray[1];
    }

}

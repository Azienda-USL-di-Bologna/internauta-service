package it.bologna.ausl.internauta.service.krint;

import it.bologna.ausl.internauta.service.interceptors.shpeck.FolderInterceptor;
import it.bologna.ausl.model.entities.logs.OperazioneKrint;
import java.util.Base64;
import java.util.logging.Level;
import javax.servlet.http.HttpServletRequest;
import org.jose4j.json.internal.json_simple.JSONObject;
import org.jose4j.json.internal.json_simple.parser.JSONParser;
import org.jose4j.json.internal.json_simple.parser.ParseException;

/**
 *
 * @author gusgus
 */
public class KrintUtils {
    public static Boolean doIHaveToKrint(HttpServletRequest request) {
        String header = request.getHeader("krint");
        if (header != null && !header.equals("")) {
            String string = new String(Base64.getDecoder().decode(header));
            JSONParser parser = new JSONParser();
            try {
                JSONObject json = (JSONObject) parser.parse(string);
                if ((Boolean) json.get("logga")) {
                    return true;
                } else {
                    return false;
                }
            } catch (ParseException ex) {
                return false;
            }
        }
        return false;
    }
}

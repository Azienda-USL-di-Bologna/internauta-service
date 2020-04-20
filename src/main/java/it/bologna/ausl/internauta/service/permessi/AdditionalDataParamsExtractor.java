package it.bologna.ausl.internauta.service.permessi;

import it.bologna.ausl.internauta.service.utils.AdditionalDataUtils;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 *
 * @author gdm
 */
@Component
public class AdditionalDataParamsExtractor {
    @Autowired
    AdditionalDataUtils additionalDataUtils;
    
    public LocalDate getDataPermesso() {
        Map<String, String> additionalData = additionalDataUtils.getAdditionalData();
        if (additionalData != null) {
            String dataPermesso = additionalData.get("dataPermesso");
            if (StringUtils.hasText(dataPermesso)) {
                return Instant.ofEpochMilli(Long.parseLong(dataPermesso)).atZone(ZoneId.systemDefault()).toLocalDate(); // java <= 8
//                return LocalDate.ofInstant(Instant.ofEpochMilli(Long.parseLong(dataPermesso)), ZoneId.systemDefault()); // java > 8
            }
        }
        return null;
    }
    
    public Boolean getEstraiStorico() {
        Map<String, String> additionalData = additionalDataUtils.getAdditionalData();
        if (additionalData != null) {
            String estraiStorico = additionalData.get("estraiStorico");
            if (StringUtils.hasText(estraiStorico)) {
                return Boolean.valueOf(estraiStorico);
            }
        }
        return false;
    }
    
    public Integer getIdProvenienzaOggetto() {
        Map<String, String> additionalData = additionalDataUtils.getAdditionalData();
        if (additionalData != null) {
            String idProvenienzaOggetto = additionalData.get("idProvenienzaOggetto");
            if (StringUtils.hasText(idProvenienzaOggetto)) {
                return Integer.parseInt(idProvenienzaOggetto);
            }
        }
        return null;
    }
}

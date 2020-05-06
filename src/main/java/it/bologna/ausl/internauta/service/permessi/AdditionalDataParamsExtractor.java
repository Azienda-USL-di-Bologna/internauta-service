package it.bologna.ausl.internauta.service.permessi;

import it.bologna.ausl.internauta.service.utils.AdditionalDataUtils;
import it.bologna.ausl.internauta.service.utils.InternautaConstants;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
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

    public List<InternautaConstants.Permessi.Ambiti> getAmbitiPermesso() {
        Map<String, String> additionalData = additionalDataUtils.getAdditionalData();
        if (additionalData != null) {
            String ambitiPermessoString = additionalData.get("ambitiPermesso");
            if (StringUtils.hasText(ambitiPermessoString)) {
                List<InternautaConstants.Permessi.Ambiti> ambitiPermesso = Arrays.asList(ambitiPermessoString.split("\\s*;\\s*"))
                        .stream().map(ambitoStr -> InternautaConstants.Permessi.Ambiti.valueOf(ambitoStr.toUpperCase())).collect(Collectors.toList());
                return ambitiPermesso;
            }
        }
        return null;
    }

    public List<InternautaConstants.Permessi.Tipi>  getTipiPermesso() {
        Map<String, String> additionalData = additionalDataUtils.getAdditionalData();
        if (additionalData != null) {
            String tipiPermessoString = additionalData.get("tipiPermesso");
            if (StringUtils.hasText(tipiPermessoString)) {
                List<InternautaConstants.Permessi.Tipi> tipiPermesso = Arrays.asList(tipiPermessoString.split("\\s*;\\s*"))
                        .stream().map(ambitoStr -> InternautaConstants.Permessi.Tipi.valueOf(ambitoStr.toUpperCase())).collect(Collectors.toList());
                return tipiPermesso;
            }
        }
        return null;
    }
}

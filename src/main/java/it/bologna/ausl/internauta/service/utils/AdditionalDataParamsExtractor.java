package it.bologna.ausl.internauta.service.utils;

import it.bologna.ausl.internauta.service.utils.AdditionalDataUtils;
import it.bologna.ausl.internauta.service.utils.InternautaConstants;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
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
    
    public ZonedDateTime getDataRiferimentoZoned() {
        Map<String, String> additionalData = additionalDataUtils.getAdditionalData();
        if (additionalData != null) {
            String dataRiferimento = additionalData.get("dataRiferimento");
            if (StringUtils.hasText(dataRiferimento)) {
                return Instant.ofEpochMilli(Long.parseLong(dataRiferimento)).atZone(ZoneId.systemDefault()); // java <= 8
//                return LocalDate.ofInstant(Instant.ofEpochMilli(Long.parseLong(dataPermesso)), ZoneId.systemDefault()); // java > 8
            }
        }
        return null;
    }
    
    public String getModalita() {
        Map<String, String> additionalData = additionalDataUtils.getAdditionalData();
        if (additionalData != null) {
            String modalita = additionalData.get("modalita");
            if (StringUtils.hasText(modalita)) {
                return modalita;
            } else {
                return null;
            }
        }
        return null;
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

    public List<InternautaConstants.Permessi.Tipi> getTipiPermesso() {
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
    
    public List<InternautaConstants.Permessi.Predicati> getPredicatiPermesso() {
        Map<String, String> additionalData = additionalDataUtils.getAdditionalData();
        if (additionalData != null) {
            String predicatiPermessoString = additionalData.get("predicatiPermesso");
            if (StringUtils.hasText(predicatiPermessoString)) {
                List<InternautaConstants.Permessi.Predicati> predicatiPermesso = Arrays.asList(predicatiPermessoString.split("\\s*;\\s*"))
                        .stream().map(predicatoStr -> InternautaConstants.Permessi.Predicati.valueOf(predicatoStr.toUpperCase())).collect(Collectors.toList());
                return predicatiPermesso;
            }
        }
        return null;
    }
    
    public Boolean getDammiPermessiVirtualiPermesso() {
        Map<String, String> additionalData = additionalDataUtils.getAdditionalData();
        if (additionalData != null) {
            String dammiPermessiVirtualiString = additionalData.get("dammiPermessiVirtuali");
            if (StringUtils.hasText(dammiPermessiVirtualiString)) {
                return Boolean.parseBoolean(dammiPermessiVirtualiString);
            }
        }
        return false;
    }
}

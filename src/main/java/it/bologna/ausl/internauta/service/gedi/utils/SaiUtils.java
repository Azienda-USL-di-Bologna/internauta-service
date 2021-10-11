/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.bologna.ausl.internauta.service.gedi.utils;

import it.bologna.ausl.internauta.service.argo.utils.FascicoloUtils;
import it.bologna.ausl.internauta.service.schedulers.FascicolatoreOutboxGediLocaleManager;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 *
 * @author Salo
 */
@Component
public class SAIUtils {

    private static final Logger log = LoggerFactory.getLogger(SAIUtils.class);

    @Autowired
    FascicoloUtils fascicoloUtils;

    @Autowired
    FascicolatoreOutboxGediLocaleManager fasicolatoreManager;

    // fascicola pec
    public String fascicolaPec(Integer idOutbox,
            Integer idAzienda,
            String codiceFiscale,
            String mittente,
            String numerazioneGerarchicaDelPadre) throws Exception {
        String idFascicoloPadre = null;
        log.info("Cerco il fascicolo padre");
        if (numerazioneGerarchicaDelPadre != null) {
            idFascicoloPadre = fascicoloUtils.getIdFascicoloByNumerazioneGerarchica(idAzienda, numerazioneGerarchicaDelPadre);
        } else {
            // cerca il fascicolo padre nei parametri aziendali
        }
        log.info("id fascicolo padre " + idFascicoloPadre);

        log.info("Cerco il fascicolo destinazione ...");
        Map<String, Object> fascicoloDestinazione = fascicoloUtils.getFascicoloByPatternInNameAndIdFascicoloPadre(idAzienda, codiceFiscale, idFascicoloPadre);
        if (fascicoloDestinazione != null) {
            log.info("fascicolo destinazione: " + fascicoloDestinazione.toString());

        } else {
            log.info("Not found fascicolo destinazione: va creato");
            // crea il fascicolo
            // fascicoloDestinazione = ....
        }

        String numerazioneFascicoloDestinazione = (String) fascicoloDestinazione.get("numerazione_gerarchica");
        log.info("Accodo mestieri di fascicolazione outbox");
        fasicolatoreManager.scheduleAutoFascicolazioneOutbox(idOutbox, idAzienda, numerazioneFascicoloDestinazione);
        return numerazioneFascicoloDestinazione;

    }

    // cerca fascicolo
    // crea fascicolo
}

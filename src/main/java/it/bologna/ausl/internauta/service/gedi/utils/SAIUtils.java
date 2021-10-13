package it.bologna.ausl.internauta.service.gedi.utils;

import it.bologna.ausl.internauta.service.argo.utils.FascicoloUtils;
import it.bologna.ausl.internauta.service.exceptions.sai.FascicoloNotFoundException;
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
        Map<String, Object> fascicoloPadre = null;
        if (numerazioneGerarchicaDelPadre != null) {
            fascicoloPadre = fascicoloUtils.getFascicoloByNumerazioneGerarchica(idAzienda, numerazioneGerarchicaDelPadre);
            if (fascicoloPadre != null) {
                idFascicoloPadre = (String) fascicoloPadre.get("id_fascicolo");
            } else {
                throw new FascicoloNotFoundException("Impossibile trovare il fascicolo " + numerazioneGerarchicaDelPadre);
            }
        } else {
            log.info("FASCICOLAZIONE GERARCHICA DEL PADRE NON PRESENTE");
            // TODO:LETTURA DEL PARAMETRO DEL FASCICOLO IN BASE AL  MITTENTE DAL DB
        }
        log.info("id fascicolo padre " + idFascicoloPadre);

        log.info("Cerco il fascicolo destinazione ...");
        Map<String, Object> fascicoloDestinazione = fascicoloUtils.getFascicoloByPatternInNameAndIdFascicoloPadre(idAzienda, codiceFiscale, idFascicoloPadre);
        if (fascicoloDestinazione != null) {
            log.info("fascicolo destinazione: " + fascicoloDestinazione.toString());
        } else {
            log.info("Not found fascicolo destinazione: va creato");
            String nomeFascicoloTemplate = "Sottofascicolo SAI di " + codiceFiscale;
            fascicoloDestinazione = createFascicoloDestinazione(idAzienda, nomeFascicoloTemplate, fascicoloPadre);
            // fascicoloDestinazione = ....
        }

        String numerazioneFascicoloDestinazione = (String) fascicoloDestinazione.get("numerazione_gerarchica");
        log.info("Accodo mestieri di fascicolazione outbox");
        fasicolatoreManager.scheduleAutoFascicolazioneOutbox(idOutbox, idAzienda, numerazioneFascicoloDestinazione);
        return numerazioneFascicoloDestinazione;

    }

    private Map<String, Object> createFascicoloDestinazione(Integer idAzienda, String codiceFiscale, Map<String, Object> fascicoloPadre) throws Exception {
        log.info("Creo fascicolo destinazione");
        return fascicoloUtils.createFascicolo(idAzienda, codiceFiscale, fascicoloPadre);
    }
    // crea fascicolo
}

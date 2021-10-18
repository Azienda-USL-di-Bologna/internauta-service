package it.bologna.ausl.internauta.service.gedi.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import it.bologna.ausl.internauta.service.argo.utils.gd.FascicoloUtils;
import it.bologna.ausl.internauta.service.exceptions.sai.FascicoloNotFoundException;
import it.bologna.ausl.internauta.service.schedulers.FascicolatoreOutboxGediLocaleManager;
import it.bologna.ausl.internauta.service.utils.ParametriAziendeReader;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import it.bologna.ausl.internauta.service.exceptions.sai.FascicoloPadreNotDefinedException;

/**
 *
 * @author Salo
 */
@Component
public class SAIUtils {

    private static final Logger log = LoggerFactory.getLogger(SAIUtils.class);

    @Autowired
    private FascicoloUtils fascicoloUtils;

    @Autowired
    private FascicolatoreOutboxGediLocaleManager fasicolatoreManager;

    @Autowired
    private ParametriAziendeReader parametriAziendeReader;

    // fascicola pec
    public String fascicolaPec(Integer idOutbox,
            Integer idAzienda,
            String codiceFiscale,
            String mittente,
            String numerazioneGerarchicaDelPadre) throws Exception {
        String idFascicoloPadre = null;
        log.info("Cerco il fascicolo padre");
        Map<String, Object> fascicoloPadre = null;
        if (numerazioneGerarchicaDelPadre == null) {
            log.info("fascicolazione gerarchida del padre non passata, la cerco in parametri_aziene");
            numerazioneGerarchicaDelPadre = getNumerazioneGerarchicaFascicoloDestinazione(mittente, idAzienda);
        }
        if (numerazioneGerarchicaDelPadre != null) {
            fascicoloPadre = fascicoloUtils.getFascicoloByNumerazioneGerarchica(idAzienda, numerazioneGerarchicaDelPadre);
            if (fascicoloPadre != null) {
                idFascicoloPadre = (String) fascicoloPadre.get("id_fascicolo");
            } else {
                throw new FascicoloNotFoundException("Impossibile trovare il fascicolo " + numerazioneGerarchicaDelPadre);
            }
        } else {
            String error = "non è stato possibile reperire la numerazione gerarchica del padre";
            log.error(error);
            throw new FascicoloPadreNotDefinedException(error);
        }
        log.info("id fascicolo padre: " + idFascicoloPadre);

        log.info("Cerco il fascicolo destinazione ...");
        Map<String, Object> fascicoloDestinazione = fascicoloUtils.getFascicoloByPatternInNameAndIdFascicoloPadre(idAzienda, codiceFiscale, idFascicoloPadre);
        if (fascicoloDestinazione != null) {
            log.info("fascicolo destinazione: " + fascicoloDestinazione.toString());
        } else {
            log.info("Not found fascicolo destinazione: va creato");
            String nomeFascicoloTemplate = "Sottofascicolo SAI di " + codiceFiscale;
            fascicoloDestinazione = createFascicoloDestinazione(idAzienda, nomeFascicoloTemplate, fascicoloPadre);

            // QUA SI DOVREBBERO DUPLICARE I PERMESSI, MA ABBIAMO DECISO DI NO
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

    private void duplicaPermessiFascicolo(Map<String, Object> fascicoloOrigine, Map<String, Object> fascicoloDestinazione) {

    }

    private String getNumerazioneGerarchicaFascicoloDestinazione(String indirizzoPec, Integer idAzienda) throws FascicoloPadreNotDefinedException {
        String res;
        Map<String, String> mappaPecFascicoli;
        try {
            mappaPecFascicoli = parametriAziendeReader.getValue(
                    parametriAziendeReader.getParameters("fascicoliSAI", new Integer[]{idAzienda}).get(0),
                    new TypeReference<Map<String, String>>() {
            });
        } catch (Exception ex) {
            throw new FascicoloPadreNotDefinedException("errore nella lettura del parametro dal database", ex);
        }
        if (mappaPecFascicoli == null || mappaPecFascicoli.isEmpty()) {
            throw new FascicoloPadreNotDefinedException(String.format("non è stato definito nessun fascicolo padre nei parametri_azienda per l'azienda passata idAzienda %d", idAzienda));
        }

        if (mappaPecFascicoli.containsKey(indirizzoPec)) {
            res = mappaPecFascicoli.get(indirizzoPec);
        } else {
            log.warn(String.format("non è stato definito nessun fascicolo padre nei parametri_azienda per la pec %s e l'azienda %d, leggo quello di default", indirizzoPec, idAzienda));
            if (mappaPecFascicoli.containsKey("default")) {
                res = mappaPecFascicoli.get("default");
            } else {
                throw new FascicoloPadreNotDefinedException(String.format("non è stato definito nessun fascicolo padre nei parametri_azienda per la pec %s e l'azienda passata", indirizzoPec));
            }
        }
        return res;
    }
}

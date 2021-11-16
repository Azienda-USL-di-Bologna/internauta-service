package it.bologna.ausl.internauta.service.argo.utils.gd;

import it.bologna.ausl.internauta.service.argo.utils.ArgoConnectionManager;
import it.bologna.ausl.internauta.service.argo.utils.IndeUtils;
import it.bologna.ausl.internauta.service.exceptions.sai.FascicolazioneGddocException;
import java.util.Date;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.sql2o.Connection;
import org.sql2o.Query;

/**
 *
 * @author Salo
 */
@Component
public class FascicoloGddocUtils {

    @Autowired
    ArgoConnectionManager argoConnectionManager;

    private static final Logger log = LoggerFactory.getLogger(FascicoloGddocUtils.class);

    public void fascicolaGddoc(Integer idAzienda, Map<String, Object> gddoc, Map<String, Object> fascicolo) throws Exception {

        String idFascicoloGddoc = IndeUtils.generateIndeID();
        String idFascicolo = (String) fascicolo.get("id_fascicolo");
        String idGddoc = (String) gddoc.get("id_gddoc");
        String idUtenteCreazione = (String) fascicolo.get("id_utente_creazione");
        Date dataCreazione = new Date();
        String queryInsert = "insert into gd.fascicoli_gddocs"
                + "(id_fascicolo_gddoc, id_fascicolo, id_gddoc, "
                + "id_utente_fascicolatore, data_assegnazione, visibile)"
                + "VALUES"
                + "(:idFascicoloGddoc, :idFascicolo, :idGddoc, "
                + ":idUtenteCreazione, :dataCreazione, -1);";
        try (Connection conn = argoConnectionManager.getConnection(idAzienda)) {
            Query createQuery = conn.createQuery(queryInsert)
                    .addParameter("idFascicoloGddoc", idFascicoloGddoc)
                    .addParameter("idFascicolo", idFascicolo)
                    .addParameter("idGddoc", idGddoc)
                    .addParameter("idUtenteCreazione", idUtenteCreazione)
                    .addParameter("dataCreazione", dataCreazione);
            createQuery.executeUpdate();
        } catch (Exception ex) {
            log.error(ex.toString());
            log.error(ex.getMessage());
            throw new FascicolazioneGddocException("Errore nella fascicolazione del gddoc\n"
                    + "gddoc: " + gddoc.toString() + "\n"
                    + "fascicolo: " + fascicolo.toString(), ex);
        }
    }

}

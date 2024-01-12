package it.bologna.ausl.model.entities.baborg.projections.struttura;

import com.querydsl.core.types.dsl.BooleanExpression;
import it.bologna.ausl.blackbox.PermissionManager;
import it.bologna.ausl.blackbox.exceptions.BlackBoxPermissionException;
import it.bologna.ausl.internauta.service.baborg.utils.BaborgUtils;
import it.bologna.ausl.internauta.service.repositories.baborg.StrutturaRepository;
import it.bologna.ausl.internauta.service.repositories.baborg.StrutturaUnificataRepository;
import it.bologna.ausl.internauta.service.repositories.baborg.UtenteRepository;
import it.bologna.ausl.internauta.service.utils.AdditionalDataParamsExtractor;
import it.bologna.ausl.internauta.model.bds.types.PermessoEntitaStoredProcedure;
import it.bologna.ausl.model.entities.baborg.QStrutturaUnificata;
import it.bologna.ausl.model.entities.baborg.Struttura;
import it.bologna.ausl.model.entities.baborg.StrutturaUnificata;
import it.bologna.ausl.model.entities.baborg.Utente;
import it.bologna.ausl.model.entities.baborg.projections.generated.UtenteWithIdPersona;
import it.bologna.ausl.model.entities.baborg.projections.strutturaunificata.StrutturaUnificataCustom;
import it.bologna.ausl.model.entities.permessi.Entita;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.json.JSONArray;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.stereotype.Component;

/**
 *
 * @author Top
 */
@Component
public class StrutturaProjectionUtils {
    
    @Autowired
    protected ProjectionFactory projectionFactory;
        
    @Autowired
    private PermissionManager permissionManager;
    
    @Autowired
    protected StrutturaRepository strutturaRepository;
    
    @Autowired
    protected UtenteRepository utenteRepository;
    
    @Autowired
    private AdditionalDataParamsExtractor additionalDataParamsExtractor;
    
    @Autowired
    protected StrutturaUnificataRepository strutturaUnificataRepository;
    
    @Autowired
    protected BaborgUtils baborgUtils;
    
    public List<PermessoEntitaStoredProcedure> getStruttureConnesseAUfficio(Struttura struttura) throws BlackBoxPermissionException {

        List<String> predicati = new ArrayList<>();
        predicati.add("CONNESSO");
        List<String> ambiti = new ArrayList<>();
        ambiti.add("BABORG");
        List<String> tipi = new ArrayList<>();
        tipi.add("UFFICIO");

        List<PermessoEntitaStoredProcedure> subjectsWithPermissionsOnObject = new ArrayList<>();
        subjectsWithPermissionsOnObject = permissionManager.getSubjectsWithPermissionsOnObject(struttura, predicati, ambiti, tipi, Boolean.FALSE);
        if (subjectsWithPermissionsOnObject != null) {
            for (PermessoEntitaStoredProcedure permessoEntitaStoredProcedure : subjectsWithPermissionsOnObject) {
                if (permessoEntitaStoredProcedure.getSoggetto().getTable().equals(Entita.TabelleTipiEntita.strutture.toString())) {
                    Struttura strutturaSoggetto = strutturaRepository.findById(permessoEntitaStoredProcedure.getSoggetto().getIdProvenienza()).get();
                    permessoEntitaStoredProcedure.getSoggetto().setDescrizione(
                            strutturaSoggetto.getNome() + (strutturaSoggetto.getCodice() != null ? " [" + strutturaSoggetto.getCodice() + "]" : ""));
                }
            }
        }

        return subjectsWithPermissionsOnObject;
    }
    
    public List<UtenteWithIdPersona> getResposabiliStruttura(Struttura struttura) {
        List<UtenteWithIdPersona> res = null;
        String idUtentiResponsabiliArray = strutturaRepository.getResponsabili(struttura.getId());
        if (idUtentiResponsabiliArray != null) {
            JSONArray array = new JSONArray(idUtentiResponsabiliArray);
            List<Integer> idUtentiResponsabili = new ArrayList();
            for (int i = 0; i < array.length(); ++i) {
                idUtentiResponsabili.add(array.optInt(i));
            }
            if (!idUtentiResponsabili.isEmpty()) {
                res = idUtentiResponsabili.stream().map(idUtenteResponsabile -> {
                    Utente utenteResposabile = utenteRepository.findById(idUtenteResponsabile).get();
                    return projectionFactory.createProjection(UtenteWithIdPersona.class, utenteResposabile);
                }).collect(Collectors.toList());
            }
        }
        return res;
    }
    
    
    /**
     * Metedo da chiamare per riempire il campo fusioni di una struttura.
     * E' necessario che in additionalData ci sia la data per fargli prendere le
     * fusioni attive in una certa data.
     * @param struttura
     * @return 
     */
    public List<StrutturaUnificataCustom> getFusioni(Struttura struttura) {
        ZonedDateTime dataRiferimento = additionalDataParamsExtractor.getDataRiferimentoZoned().truncatedTo(ChronoUnit.DAYS);
        
        return baborgUtils.getUnificazione(struttura, dataRiferimento, "FUSIONE");
    }
    
    /**
     * Metodo da chiamare per riempire il campo replice di una struttura.
     * E' necessario che in additionalData ci sia la data per fargli prendere le
     * replice attive in una certa data.
     * @param struttura
     * @return 
     */
    public List<Struttura> getStruttureReplicheList(Struttura struttura) {
        ZonedDateTime dataRiferimento = additionalDataParamsExtractor.getDataRiferimentoZoned().truncatedTo(ChronoUnit.DAYS);
        
        return baborgUtils.getStruttureUnificate(struttura, dataRiferimento, "REPLICA");
    }
}

package it.bologna.ausl.internauta.service.krint;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import it.bologna.ausl.blackbox.PermissionRepositoryAccess;
import it.bologna.ausl.blackbox.exceptions.BlackBoxPermissionException;
import it.bologna.ausl.blackbox.utils.BlackBoxConstants;
import it.bologna.ausl.internauta.service.repositories.baborg.StrutturaRepository;
import it.bologna.ausl.internauta.service.utils.InternautaConstants;
import it.bologna.ausl.internauta.utils.bds.types.CategoriaPermessiStoredProcedure;
import it.bologna.ausl.internauta.utils.bds.types.PermessoEntitaStoredProcedure;
import it.bologna.ausl.internauta.utils.bds.types.PermessoStoredProcedure;
import it.bologna.ausl.model.entities.baborg.Struttura;
import it.bologna.ausl.model.entities.logs.OperazioneKrint;
import java.util.Base64;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import org.jose4j.json.internal.json_simple.JSONObject;
import org.jose4j.json.internal.json_simple.parser.JSONParser;
import org.jose4j.json.internal.json_simple.parser.ParseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 *
 * @author gusgus
 */
@Component
public class KrintUtils {

    @Autowired
    private StrutturaRepository strutturaRepository;

    @Autowired
    private KrintBaborgService krintBaborgService;

    @Autowired
    private PermissionRepositoryAccess permissionRepositoryAccess;
    
    @Autowired
    private KrintScriptaService krintScriptaService;
    
    @Autowired
    ObjectMapper objectMapper;

    public Boolean doIHaveToKrint(HttpServletRequest request) {
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
    /**
     * Metodo che effettua il controllo dei permessi passati in ingresso con quelli salvati sul DB e scrive nella tabella del krint per i log.
     * Verifica se i permessi sono stati aggiunti/aggiornati/rimossi e scrive lo specifico messaggio.
     * @param permessiEntitaStoredProcedureDaSalvare La lista dei permessi da salvare.
     */
    public void manageKrintPermissions(List<PermessoEntitaStoredProcedure> permessiEntitaStoredProcedureDaSalvare) {

        /* Il metodo inizializza tutte le liste dei permessi da aggiungere/aggiornare/rimuovere con la lista dei permessi da salvare e per ogni controllo
         * rimuove da una specifica lista il permesso nel caso in cui non viene verificata la condizione per cui l'elemento deve essere aggiunto/aggiornato/rimosso.        
         */
        List<PermessoEntitaStoredProcedure> permessiDaAggiungere = objectMapper.convertValue(permessiEntitaStoredProcedureDaSalvare, new TypeReference<List<PermessoEntitaStoredProcedure>>(){});
        List<PermessoEntitaStoredProcedure> permessiDaAggiornare = objectMapper.convertValue(permessiEntitaStoredProcedureDaSalvare, new TypeReference<List<PermessoEntitaStoredProcedure>>(){});
        List<PermessoEntitaStoredProcedure> permessiDaRimuovere = objectMapper.convertValue(permessiEntitaStoredProcedureDaSalvare, new TypeReference<List<PermessoEntitaStoredProcedure>>(){});
        boolean isRestoreOperation = false;
        // I permessi sono nel formato della blackbox e possono essere per più soggetti/oggetti.
        for (PermessoEntitaStoredProcedure permessoEntitaStoredDaSalvare : permessiEntitaStoredProcedureDaSalvare) {
            // Su un permesso entita possono esserci più categorie, ovvero permessi di più ambiti o tipi applicativi
            for (CategoriaPermessiStoredProcedure categoriaPermessiStoredProcedure : permessoEntitaStoredDaSalvare.getCategorie()) {
                List<PermessoEntitaStoredProcedure> permessiAttualiSuBlackBox = null;
                try{
                    permessiAttualiSuBlackBox = permissionRepositoryAccess.getPermissionsOfSubjectAdvanced(
                            permessoEntitaStoredDaSalvare.getSoggetto(), 
                            Lists.newArrayList(permessoEntitaStoredDaSalvare.getOggetto()),
                            null,
                            Lists.newArrayList(categoriaPermessiStoredProcedure.getAmbito()),
                            Lists.newArrayList(categoriaPermessiStoredProcedure.getTipo()),
                            true,
                            null, 
                            null,
                            BlackBoxConstants.Direzione.PRESENTE);
                }catch (BlackBoxPermissionException ex){
                    return;
                }    
                if (permessiAttualiSuBlackBox != null && !permessiAttualiSuBlackBox.isEmpty()) {
                    for (PermessoEntitaStoredProcedure attualiPermessoEntita : permessiAttualiSuBlackBox) {
                        for (CategoriaPermessiStoredProcedure attualiCategoria : attualiPermessoEntita.getCategorie()) {
                            
                            /* In questo ciclo viene fatto il controllo dei permessi da salvare con quelli salvati sul DB per distinguere quali permessi
                             * devono essere aggiunti, quali aggiornati e quali rimossi.*/
                            // Controllo permessi da aggiungere = permessi nuovi - permessi attuali
                            List<PermessoStoredProcedure> permessiDaAggiungereTemp = permessiDaAggiungere.get(permessiEntitaStoredProcedureDaSalvare.indexOf(permessoEntitaStoredDaSalvare)).getCategorie().get(permessoEntitaStoredDaSalvare.getCategorie().indexOf(categoriaPermessiStoredProcedure)).getPermessi();
                            for(PermessoStoredProcedure permessoAttuale : attualiCategoria.getPermessi()){
                                isRestoreOperation = checkIsRestoreOperation(
                                        attualiPermessoEntita.getSoggetto().getIdProvenienza(),
                                        permessiDaAggiungere.get(permessiEntitaStoredProcedureDaSalvare.indexOf(permessoEntitaStoredDaSalvare)).getSoggetto().getIdProvenienza(),
                                        attualiPermessoEntita.getOggetto().getIdProvenienza(), 
                                        permessiDaAggiungere.get(permessiEntitaStoredProcedureDaSalvare.indexOf(permessoEntitaStoredDaSalvare)).getOggetto().getIdProvenienza(),
                                        permessiDaAggiungereTemp);
                                
                                permessiDaAggiungereTemp.removeIf(permesso -> (permesso.getPredicato().equalsIgnoreCase(permessoAttuale.getPredicato()) ||
                                        checkTipoEPredicatiPermesso(categoriaPermessiStoredProcedure.getTipo(), permesso, permessoAttuale)));
                            }
                            
                            // Controllo permessi da cancellare = permessi attuali - permessi nuovi
                            List<PermessoStoredProcedure> permessiAttualiClone = objectMapper.convertValue(attualiCategoria.getPermessi(), new TypeReference<List<PermessoStoredProcedure>>(){});
                            for(PermessoStoredProcedure permessoNuovo : categoriaPermessiStoredProcedure.getPermessi()){
                                permessiAttualiClone.removeIf(permesso -> (permesso.getPredicato().equalsIgnoreCase(permessoNuovo.getPredicato()) || 
                                        checkTipoEPredicatiPermesso(categoriaPermessiStoredProcedure.getTipo(), permesso, permessoNuovo)));
                            }
                            permessiDaRimuovere.get(permessiEntitaStoredProcedureDaSalvare.indexOf(permessoEntitaStoredDaSalvare)).getCategorie().get(permessoEntitaStoredDaSalvare.getCategorie().indexOf(categoriaPermessiStoredProcedure)).setPermessi(permessiAttualiClone);
                            
                            // Permessi da aggiornare = permessi nuovi - permessi da inserire
                            List<PermessoStoredProcedure> permessiNuoviClone = objectMapper.convertValue(categoriaPermessiStoredProcedure.getPermessi(), new TypeReference<List<PermessoStoredProcedure>>(){});
                            for(PermessoStoredProcedure permessoDaInserire : permessiDaAggiungereTemp){
                                permessiNuoviClone.removeIf(permesso -> (permesso.getPredicato().equalsIgnoreCase(permessoDaInserire.getPredicato())));
                            }
                            for(PermessoStoredProcedure permessoAttuale : attualiCategoria.getPermessi()){
                                permessiNuoviClone.removeIf(permesso -> (permesso.equals(permessoAttuale)));
                            }
                            permessiDaAggiornare.get(permessiEntitaStoredProcedureDaSalvare.indexOf(permessoEntitaStoredDaSalvare)).getCategorie().get(permessoEntitaStoredDaSalvare.getCategorie().indexOf(categoriaPermessiStoredProcedure)).setPermessi(permessiNuoviClone);
                        }
                    }
                }else{
                    permessiDaRimuovere.get(permessiEntitaStoredProcedureDaSalvare.indexOf(permessoEntitaStoredDaSalvare)).getCategorie().get(permessoEntitaStoredDaSalvare.getCategorie().indexOf(categoriaPermessiStoredProcedure)).getPermessi().clear();
                    permessiDaAggiornare.get(permessiEntitaStoredProcedureDaSalvare.indexOf(permessoEntitaStoredDaSalvare)).getCategorie().get(permessoEntitaStoredDaSalvare.getCategorie().indexOf(categoriaPermessiStoredProcedure)).getPermessi().clear();
                }
                
            }
        }

        //ciclo la lista di permessiDaAggiornare
        for (PermessoEntitaStoredProcedure permessi : permessiDaAggiornare) {
            //ciclo le categorie
            for (CategoriaPermessiStoredProcedure categoriaPermessiStoredProcedure : permessi.getCategorie()) {
                //ciclo i effettivi permessi
                for (PermessoStoredProcedure permessoStoredProcedure : categoriaPermessiStoredProcedure.getPermessi()) {

                    //controllo che si tratti di un permeso di connessione(struttura ufficio)
                    if (permessoStoredProcedure.getPredicato().equals(InternautaConstants.Permessi.Predicati.CONNESSO.toString())) {
                        Struttura strutturaSoggetto = strutturaRepository.findById(permessi.getSoggetto().getIdProvenienza()).get();
                        Struttura strutturaOggetto = strutturaRepository.findById(permessi.getOggetto().getIdProvenienza()).get();
                        OperazioneKrint.CodiceOperazione cod = null;
                        
                        //controllo se è stato aggiunto il propaga o tolto
                        if (!permessoStoredProcedure.getPropagaSoggetto()) {
                            cod = OperazioneKrint.CodiceOperazione.BABORG_UFFICIO_STRUTTURE_CONNESSE_LIST_PROPAGA_REMOVE;
                        } else if (permessoStoredProcedure.getPropagaSoggetto()) {
                            cod = OperazioneKrint.CodiceOperazione.BABORG_UFFICIO_STRUTTURE_CONNESSE_LIST_PROPAGA_ADD;
                        }
                        krintBaborgService.writeUfficioUpdate(strutturaOggetto, cod, strutturaSoggetto);
                    }
                    
                    //controllo che si tratti di un permesso sugli archivi
                    if (categoriaPermessiStoredProcedure.getTipo().equals(InternautaConstants.Permessi.Tipi.ARCHIVIO.toString())){
                        if (permessoStoredProcedure.getPredicato().equals(InternautaConstants.Permessi.Predicati.VISUALIZZA.toString())
                            || permessoStoredProcedure.getPredicato().equals(InternautaConstants.Permessi.Predicati.MODIFICA.toString())
                            || permessoStoredProcedure.getPredicato().equals(InternautaConstants.Permessi.Predicati.ELIMINA.toString())
                            || permessoStoredProcedure.getPredicato().equals(InternautaConstants.Permessi.Predicati.BLOCCO.toString())){
                            
                            OperazioneKrint.CodiceOperazione codiceOp = "persone".equals(permessi.getSoggetto().getTable()) ? 
                                    OperazioneKrint.CodiceOperazione.SCRIPTA_ARCHIVIO_PERMESSI_PERSONA_UPDATE :
                                    OperazioneKrint.CodiceOperazione.SCRIPTA_ARCHIVIO_PERMESSI_STRUTTURA_UPDATE;
                            krintScriptaService.writePermessiArchivio(
                                    permessi.getOggetto().getIdProvenienza(), 
                                    permessi.getSoggetto(),
                                    permessoStoredProcedure, 
                                    codiceOp);
                        }
                    }
                        
                }
            }
        }
        
        //ciclo la lista di permessiDaRimuovere
        for (PermessoEntitaStoredProcedure permessi : permessiDaRimuovere) {
            //ciclo le categorie
            for (CategoriaPermessiStoredProcedure categoriaPermessiStoredProcedure : permessi.getCategorie()) {
                //ciclo i effettivi permessi
                for (PermessoStoredProcedure permessoStoredProcedure : categoriaPermessiStoredProcedure.getPermessi()) {

                    //controllo che si tratti di un permeso di connessione(struttura ufficio)
                    if (permessoStoredProcedure.getPredicato().equals(InternautaConstants.Permessi.Predicati.CONNESSO.toString())) {
                        Struttura strutturaSoggetto = strutturaRepository.findById(permessi.getSoggetto().getIdProvenienza()).get();
                        Struttura strutturaOggetto = strutturaRepository.findById(permessi.getOggetto().getIdProvenienza()).get();
                        OperazioneKrint.CodiceOperazione cod = OperazioneKrint.CodiceOperazione.BABORG_UFFICIO_STRUTTURE_CONNESSE_LIST_REMOVE;

                        krintBaborgService.writeUfficioUpdate(strutturaOggetto, cod, strutturaSoggetto);
                    }
                    
                    //controllo che si tratti di un permesso sugli archivi
                    if (categoriaPermessiStoredProcedure.getTipo().equals(InternautaConstants.Permessi.Tipi.ARCHIVIO.toString())){
                        
                       OperazioneKrint.CodiceOperazione codiceOp = isRestoreOperation ? OperazioneKrint.CodiceOperazione.SCRIPTA_ARCHIVIO_PERMESSI_EREDITATI_RESTORE : 
                                OperazioneKrint.CodiceOperazione.SCRIPTA_ARCHIVIO_PERMESSI_DELETE;
                        
                        if (permessoStoredProcedure.getPredicato().equals(InternautaConstants.Permessi.Predicati.VISUALIZZA.toString())
                            || permessoStoredProcedure.getPredicato().equals(InternautaConstants.Permessi.Predicati.MODIFICA.toString())
                            || permessoStoredProcedure.getPredicato().equals(InternautaConstants.Permessi.Predicati.ELIMINA.toString())
                            || permessoStoredProcedure.getPredicato().equals(InternautaConstants.Permessi.Predicati.BLOCCO.toString())){
                            krintScriptaService.writePermessiArchivio(
                                    permessi.getOggetto().getIdProvenienza(), 
                                    permessi.getSoggetto(), 
                                    permessoStoredProcedure,
                                    codiceOp);
                        }
                    }
                }
            }
        }
        
        //ciclo la lista di permessiDaAggiungere
        for (PermessoEntitaStoredProcedure permessi : permessiDaAggiungere) {
            //ciclo le categorie
            for (CategoriaPermessiStoredProcedure categoriaPermessiStoredProcedure : permessi.getCategorie()) {
                //ciclo i effettivi permessi
                for (PermessoStoredProcedure permessoStoredProcedure : categoriaPermessiStoredProcedure.getPermessi()) {

                    //controllo che si tratti di un permeso di connessione(struttura ufficio)
                    if (permessoStoredProcedure.getPredicato().equals(InternautaConstants.Permessi.Predicati.CONNESSO.toString())) {
                        Struttura strutturaSoggetto = strutturaRepository.findById(permessi.getSoggetto().getIdProvenienza()).get();
                        Struttura strutturaOggetto = strutturaRepository.findById(permessi.getOggetto().getIdProvenienza()).get();
                        OperazioneKrint.CodiceOperazione cod = OperazioneKrint.CodiceOperazione.BABORG_UFFICIO_STRUTTURE_CONNESSE_LIST_ADD;

                        krintBaborgService.writeUfficioUpdate(strutturaOggetto, cod, strutturaSoggetto);
                    }
                    
                    //controllo che si tratti di un permesso sugli archivi
                    if (categoriaPermessiStoredProcedure.getTipo().equals(InternautaConstants.Permessi.Tipi.ARCHIVIO.toString())){
                        if (permessoStoredProcedure.getPredicato().equals(InternautaConstants.Permessi.Predicati.VISUALIZZA.toString())
                            || permessoStoredProcedure.getPredicato().equals(InternautaConstants.Permessi.Predicati.MODIFICA.toString())
                            || permessoStoredProcedure.getPredicato().equals(InternautaConstants.Permessi.Predicati.ELIMINA.toString())
                            || permessoStoredProcedure.getPredicato().equals(InternautaConstants.Permessi.Predicati.BLOCCO.toString())){
                            
                            OperazioneKrint.CodiceOperazione codiceOp = "persone".equals(permessi.getSoggetto().getTable()) ? 
                                    OperazioneKrint.CodiceOperazione.SCRIPTA_ARCHIVIO_PERMESSI_PERSONA_CREATION :
                                    OperazioneKrint.CodiceOperazione.SCRIPTA_ARCHIVIO_PERMESSI_STRUTTURA_CREATION;
                            krintScriptaService.writePermessiArchivio(
                                    permessi.getOggetto().getIdProvenienza(), 
                                    permessi.getSoggetto(), 
                                    permessoStoredProcedure, 
                                    codiceOp);
                        }
                    }
                }
            }
        }
    }
    
    /**
     * Metodo che controlla se il tipo del permesso è di Archivio e che i permessi due permessi passati in ingresso sono 
     * entrambi del tipo [VISUALIZZA,MODIFICA,ELIMINA,BLOCCO].
     * @param categoriaPermessiStoredProcedure La categoria permessi.
     * @param permesso Il primo permesso da controllare.
     * @param permessoNuovo Il secondo permesso da controllare.
     * @return {@code true} o {@code false}.
     */
    private boolean checkTipoEPredicatiPermesso(String tipoOggettoCategoria, PermessoStoredProcedure permesso, PermessoStoredProcedure permessoNuovo) {
        return InternautaConstants.Permessi.Tipi.ARCHIVIO.toString().equals(tipoOggettoCategoria)
                && Lists.newArrayList(
                        InternautaConstants.Permessi.Predicati.VISUALIZZA.toString(),
                        InternautaConstants.Permessi.Predicati.MODIFICA.toString(),
                        InternautaConstants.Permessi.Predicati.ELIMINA.toString(),
                        InternautaConstants.Permessi.Predicati.BLOCCO.toString()).containsAll(Lists.newArrayList(permesso.getPredicato(), permessoNuovo.getPredicato()));
    }
    
    /**
     * Metodo che controlla se stiamo effettuando l'operazione di RESTORE di un permesso propagato da un fascicolo padre.
     * Sul fascicolo che stiamo modificando il permesso è NON_PROPAGATO, però nel metodo che carica i permessi dal DB
     * viene caricato il permesso del padre.
     * Faremo un check sugli id dei fascicoli per capire se siamo in quella situazione, controllando anche il Predicato per escludere
     * il caso in cui stiamo "bloccando" l'ereditarietà del permesso del padre al figlio.
     * @param idSoggettoAttuale Id del soggetto caricato dalla blackbox.
     * @param idSoggettoNuovo Id del soggetto al quale stiamo modificando il permesso.
     * @param idProvenienzaAttuale Id del fascicolo caricato dalla blackbox.
     * @param idProvenienzaNuovo Id del fascicolo sul quale stiamo modificando il permesso.
     * @param permessiDaAggiungereTemp La lista dei permessi nuovi passati dal frontend.
     * @return {@code true} se è un'operazione di restore 
     */
    private boolean checkIsRestoreOperation(Integer idSoggettoAttuale, Integer idSoggettoNuovo, Integer idProvenienzaAttuale, Integer idProvenienzaNuovo, List<PermessoStoredProcedure> permessiDaAggiungereTemp) {
        // Quando rimuoviamo il permesso ereditato dal padre viene passato come permesso da aggiungere il "NON_PROPAGATO".
        return  idSoggettoAttuale.equals(idSoggettoNuovo) &&
                !idProvenienzaAttuale.equals(idProvenienzaNuovo) && 
                !permessiDaAggiungereTemp.stream().filter(p -> "NON_PROPAGATO".equals(p.getPredicato())).findFirst().isPresent();
    }
}

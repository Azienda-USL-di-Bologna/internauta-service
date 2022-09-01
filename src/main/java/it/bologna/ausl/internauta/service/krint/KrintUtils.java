package it.bologna.ausl.internauta.service.krint;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import it.bologna.ausl.blackbox.PermissionRepositoryAccess;
import it.bologna.ausl.blackbox.exceptions.BlackBoxPermissionException;
import it.bologna.ausl.blackbox.utils.BlackBoxConstants;
import it.bologna.ausl.internauta.service.interceptors.shpeck.FolderInterceptor;
import it.bologna.ausl.internauta.service.permessi.PermessoError;
import it.bologna.ausl.internauta.service.repositories.baborg.StrutturaRepository;
import it.bologna.ausl.internauta.service.utils.InternautaConstants;
import it.bologna.ausl.internauta.service.utils.NonCachedEntities;
import it.bologna.ausl.internauta.utils.bds.types.CategoriaPermessiStoredProcedure;
import it.bologna.ausl.internauta.utils.bds.types.EntitaStoredProcedure;
import it.bologna.ausl.internauta.utils.bds.types.PermessoEntitaStoredProcedure;
import it.bologna.ausl.internauta.utils.bds.types.PermessoStoredProcedure;
import it.bologna.ausl.model.entities.baborg.Struttura;
import it.bologna.ausl.model.entities.logs.OperazioneKrint;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import org.hibernate.mapping.Collection;
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

    public void manageKrintPermissions(List<PermessoEntitaStoredProcedure> permessiList) {
        
        List<PermessoEntitaStoredProcedure> permessiDaAggiungere = objectMapper.convertValue(permessiList, new TypeReference<List<PermessoEntitaStoredProcedure>>(){});
        List<PermessoEntitaStoredProcedure> permessiDaAggiornare = objectMapper.convertValue(permessiList, new TypeReference<List<PermessoEntitaStoredProcedure>>(){});
        List<PermessoEntitaStoredProcedure> permessiDaRimuovere = objectMapper.convertValue(permessiList, new TypeReference<List<PermessoEntitaStoredProcedure>>(){});
        
        for (PermessoEntitaStoredProcedure permessi : permessiList) {
            //ciclo le categorie
            for (CategoriaPermessiStoredProcedure categoriaPermessiStoredProcedure : permessi.getCategorie()) {
                List<PermessoEntitaStoredProcedure> attuali = null;
                try{
                    attuali = permissionRepositoryAccess.getPermissionsOfSubjectAdvanced(
                            permessi.getSoggetto(), 
                            Lists.newArrayList(permessi.getOggetto()),
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
                if (attuali != null && !attuali.isEmpty()) {
                    for (PermessoEntitaStoredProcedure attualiPermessoEntita : attuali) {
                        for (CategoriaPermessiStoredProcedure attualiCategoria : attualiPermessoEntita.getCategorie()) {
                            
                            //controllo se sto aggiungendo/modificando o rimuovendo permessi
                            //permessi da aggiungiere = permessi nuovi - permessi attuali
                            List<PermessoStoredProcedure> permessiDaAggiungereTemp = permessiDaAggiungere.get(permessiList.indexOf(permessi)).getCategorie().get(permessi.getCategorie().indexOf(categoriaPermessiStoredProcedure)).getPermessi();
                            for(PermessoStoredProcedure permessoAttuale : attualiCategoria.getPermessi()){
                                permessiDaAggiungereTemp.removeIf(permesso -> (permesso.getPredicato().equalsIgnoreCase(permessoAttuale.getPredicato())));
                            }
                            
                            //permessi da cancellare = permessi attuali - permessi nuovi
                            List<PermessoStoredProcedure> permessiAttualiClone = objectMapper.convertValue(attualiCategoria.getPermessi(), new TypeReference<List<PermessoStoredProcedure>>(){});
                            for(PermessoStoredProcedure permessoNuovo : categoriaPermessiStoredProcedure.getPermessi()){
                                permessiAttualiClone.removeIf(permesso -> (permesso.getPredicato().equalsIgnoreCase(permessoNuovo.getPredicato())));
                            }
                            permessiDaRimuovere.get(permessiList.indexOf(permessi)).getCategorie().get(permessi.getCategorie().indexOf(categoriaPermessiStoredProcedure)).setPermessi(permessiAttualiClone);
                            
                            //permessi da aggiornare = permessi nuovi - permessi da inserire
                            List<PermessoStoredProcedure> permessiNuoviClone = objectMapper.convertValue(categoriaPermessiStoredProcedure.getPermessi(), new TypeReference<List<PermessoStoredProcedure>>(){});
                            for(PermessoStoredProcedure permessoDaInserire : permessiDaAggiungereTemp){
                                permessiNuoviClone.removeIf(permesso -> (permesso.getPredicato().equalsIgnoreCase(permessoDaInserire.getPredicato())));
                            }
                            for(PermessoStoredProcedure permessoAttuale : attualiCategoria.getPermessi()){
                                permessiNuoviClone.removeIf(permesso -> (permesso.equals(permessoAttuale)));
                            }
                            permessiDaAggiornare.get(permessiList.indexOf(permessi)).getCategorie().get(permessi.getCategorie().indexOf(categoriaPermessiStoredProcedure)).setPermessi(permessiNuoviClone);
                        }
                    }
                }else{
                    permessiDaRimuovere.get(permessiList.indexOf(permessi)).getCategorie().get(permessi.getCategorie().indexOf(categoriaPermessiStoredProcedure)).getPermessi().clear();
                    permessiDaAggiornare.get(permessiList.indexOf(permessi)).getCategorie().get(permessi.getCategorie().indexOf(categoriaPermessiStoredProcedure)).getPermessi().clear();
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
                }
            }
        }
    }
}

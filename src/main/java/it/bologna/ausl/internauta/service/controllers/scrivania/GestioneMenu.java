package it.bologna.ausl.internauta.service.controllers.scrivania;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import it.bologna.ausl.blackbox.PermissionManager;
import it.bologna.ausl.blackbox.exceptions.BlackBoxPermissionException;
import it.bologna.ausl.internauta.service.authorization.UserInfoService;
import it.bologna.ausl.internauta.service.repositories.scrivania.BmenuRepository;
import it.bologna.ausl.internauta.service.utils.InternautaConstants;
import it.bologna.ausl.model.entities.baborg.Persona;
import it.bologna.ausl.model.entities.baborg.Ruolo;
import it.bologna.ausl.model.entities.baborg.Utente;
import it.bologna.ausl.model.entities.scrivania.Bmenu;
import it.bologna.ausl.model.entities.scrivania.QBmenu;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 *
 * @author gusgus
 */
@Component
public class GestioneMenu {
    
    @Autowired
    UserInfoService userInfoService;
    
    @Autowired
    BmenuRepository bmenuRepository;
    
    @Autowired
    PermissionManager permissionManager;
    
    
    /**
     * Esegue la query delle voci di menu filtrate in base ai parametri passati.
     * Se la voce di menu è permessa tramite deleghe allora viene guardato se l'utente ha la delega.
     * @param livello
     * @param idPadre 
     */
    private List<Bmenu> selectFromMenu(
            Integer livello, 
            Integer idPadre, 
            Integer idAzienda, 
            List<String> permessiDiFlusso, 
            List<String> ruoli, 
            Utente utente) {
        QBmenu bmenu = QBmenu.bmenu;
        
        // CONDIZIONE LIVELLO
        BooleanExpression whereCondition = bmenu.livello.eq(livello);

        if (idPadre != null) {
            // CONDIZIONE PADRE
            whereCondition = whereCondition.and(bmenu.idPadre.id.eq(idPadre));
        }
        
        // CONDIZIONE AZIENDA
        whereCondition = whereCondition.and(
            bmenu.aziende.isNull().or(
                Expressions.booleanTemplate("tools.array_overlap({0}, string_to_array({1}, ','))=true", bmenu.aziende, idAzienda.toString())       
            )
        );
        
        // CONDIZIONE PERMESSI E RUOLI OPPURE DELEGA
        BooleanExpression whereConditionPermessiRuoliDeleghe =  
                bmenu.ruoliSufficienti.isNull().and(bmenu.permessiSufficienti.isNull())
                .or(bmenu.delega.isNotNull());
        if (permessiDiFlusso != null) {
            whereConditionPermessiRuoliDeleghe = whereConditionPermessiRuoliDeleghe.or(
                Expressions.booleanTemplate("tools.array_overlap({0}, string_to_array({1}, ','))=true",
                            bmenu.permessiSufficienti, String.join(",", permessiDiFlusso))
            );
        }
        if (ruoli != null) {
            whereConditionPermessiRuoliDeleghe = whereConditionPermessiRuoliDeleghe.or(
                Expressions.booleanTemplate("tools.array_overlap({0}, string_to_array({1}, ','))=true",
                                bmenu.ruoliSufficienti, String.join(",", ruoli))
            );
        }
        whereCondition = whereCondition.and(whereConditionPermessiRuoliDeleghe);
//        whereCondition = whereCondition.and(
//            bmenu.ruoliSufficienti.isNull().and(bmenu.permessiSufficienti.isNull())
//                .or(bmenu.delega.isNotNull())
//                .or(
//                    Expressions.booleanTemplate("tools.array_overlap({0}, string_to_array({1}, ','))=true",
//                                bmenu.permessiSufficienti, String.join(",", permessiDiFlusso))
//                )
//                .or(
//                    Expressions.booleanTemplate("tools.array_overlap({0}, string_to_array({1}, ','))=true",
//                                bmenu.ruoliSufficienti, String.join(",", ruoli))
//                )
//        );
        
        Iterable<Bmenu> vociMenu = bmenuRepository.findAll(whereCondition);
        List<Bmenu> vociMenuList = StreamSupport.stream(vociMenu.spliterator(), false).collect(Collectors.toList());
        vociMenuList.removeIf(voce -> !vocePermessa(voce, utente, permessiDiFlusso, ruoli));
        return vociMenuList;
    }
    
    /**
     * Questa funzione risponde all'esigenza di rimuovere eventuali voci di menu
     * che avevano come unica buona condizione il fatto di essere delegate.
     * La voce è permessa quindi per una qualsiasi di queste condizioni:
     * 1- Ha il campo delega null.
     * 2- Non servono permessi o ruoli.
     * 3- L'utente ha i permessi/ruoli.
     * 4- L'utente ha la delega.
     * @param voce
     * @return 
     */
    private Boolean vocePermessa(Bmenu voce, Utente utente, List<String> permessiDiFlusso, List<String> ruoli) {
        // 1- Ha il campo delega null.
        if (voce.getDelega() == null) {
            return true;
        }
        
        // 2- Non servono permessi o ruoli.
        if (voce.getPermessiSufficienti() == null && voce.getRuoliSufficienti() == null) {
            return true;
        }
        
        // 3- L'utente ha i permessi/ruoli.
        if (!Collections.disjoint(Arrays.asList(voce.getPermessiSufficienti()), permessiDiFlusso)
                || !Collections.disjoint(Arrays.asList(voce.getRuoliSufficienti()), ruoli)) {
            return true;
        }
        
        // 4- L'utente ha la delega.
        // TODO
        
        return false;
    }
    
    /**
     * Si occupa di far diventare una bvoce in un itemMenu.
     * Controlla anche se il menu passato contiene già quel determinato item.
     * Se la bvoce deve essere scomposta per azienda allora viene aggiunto all'item un figlio che è l'azienda.
     * @param menu
     * @param bvoce
     * @param utente
     * @return Trona l'item trovato/creato che rappresenta la bvoce
     */
    private ItemMenu buildaVoce(List<ItemMenu> menu, Bmenu bvoce, Utente utente) {
        ItemMenu voceBuildata;
        List<ItemMenu> vocemenuList = menu.stream().filter(itemvoce -> itemvoce.getId().equals(bvoce.getId())).collect(Collectors.toList());
        
        if (vocemenuList.isEmpty()) {
            voceBuildata = new ItemMenu(bvoce.getId(), bvoce.getDescrizione(), buildaUrl(), bvoce.getIcona(), null);
            if (bvoce.getScomponiPerAzienda()) {
                voceBuildata.setChildren(new ArrayList(Arrays.asList(new ItemMenu(null, utente.getIdAzienda().getNome(), buildaUrl(), null, null))));
            }
            menu.add(voceBuildata);
        } else {
            voceBuildata = vocemenuList.get(0);
            if (bvoce.getScomponiPerAzienda()) { // Se è da scomporre è sicuramente anche foglia eh...
                List<ItemMenu> children = voceBuildata.getChildren();
                children.add(new ItemMenu(null, utente.getIdAzienda().getNome(), buildaUrl(), null, null));
            }
        }
        return voceBuildata;
    }
    
    /**
     * 
     * @return 
     */
    private String buildaUrl() {
        return "";
    }
    
    
    /**
     * A partire da una voce del menu viene creato l'itemMenu relativo.
     * Viene poi chiamata ricorsivamente se quella voce di menu ha dei figli.
     * @param voci
     * @param livello
     * @param permessiDiFlusso
     * @param ruoli
     * @param menu
     * @param utente 
     */
    private void cycleBuildAndDig(
            List<Bmenu> voci, 
            Integer livello, 
            List<String> permessiDiFlusso, 
            List<String> ruoli, 
            List<ItemMenu> menu,
            Utente utente) {
        for (Bmenu voce : voci) {
            
            ItemMenu menuPadre = buildaVoce(menu, voce, utente);
            
            if (!voce.getFoglia()) {
                if (menuPadre.getChildren() == null) {
                    menuPadre.setChildren(new ArrayList());
                }
                List<ItemMenu> children = menuPadre.getChildren();
                livello++;
                List<Bmenu> sottobmenu = selectFromMenu(livello, voce.getId(), utente.getIdAzienda().getId(), permessiDiFlusso, ruoli, utente);
                cycleBuildAndDig(sottobmenu, livello, permessiDiFlusso, ruoli, children, utente);
            }
        }
    }
    
    /**
     * Creazione del menu della scrivania internauta
     * @param persona 
     * @return  
     * @throws it.bologna.ausl.blackbox.exceptions.BlackBoxPermissionException 
     */
    public List<ItemMenu> buildMenu(Persona persona, Utente uu) throws BlackBoxPermissionException {
        
//        List<Utente> utentiAttivi = userInfoService.getUtentiPersona(persona).stream().filter(u -> u.getAttivo()).collect(Collectors.toList());
        List<Utente> utentiPersona = userInfoService.getUtentiPersona(persona); // getUtentiPersonaByUtente(uu, true); //
        List<Utente> utentiAttivi = utentiPersona.stream().filter(
                u->u.getAttivo()
        ).collect(Collectors.toList());
        
        List<ItemMenu> menu = new ArrayList();
        
        // Variabili per filtrare i permessi sulla BlackBox
        List<String> ambitiFlusso = new ArrayList();
        ambitiFlusso.add(InternautaConstants.Permessi.Ambiti.PICO.toString());
        ambitiFlusso.add(InternautaConstants.Permessi.Ambiti.DETE.toString());
        ambitiFlusso.add(InternautaConstants.Permessi.Ambiti.DELI.toString());
        List<String> tipi = new ArrayList();
        tipi.add(InternautaConstants.Permessi.Tipi.FLUSSO.toString());
        
        for (Utente utente : utentiAttivi) {
            // Prendo ruoli e permessi dell'utente. (Tra i ruoli ci sono anche gli interaziendali)
            Set<Ruolo> ruoliGenerali = userInfoService.getRuoliGenerali(utente, null);
            List<String> ruoli = ruoliGenerali.stream().map(ruolo -> ruolo.getNomeBreve().toString()).collect(Collectors.toList());
            List<String> permessiDiFlusso = permissionManager.getPermission(utente, ambitiFlusso, tipi);
            
            // Eseguo la Select del livello uno del menu
            List<Bmenu> bmenu = selectFromMenu(1, null, utente.getIdAzienda().getId(), permessiDiFlusso, ruoli, utente);
            
            // Buildo le voci di menu trovate e le loro ramificazioni
            cycleBuildAndDig(bmenu, 1, permessiDiFlusso, ruoli, menu, utente);
        }
        
        return menu;
    }
}

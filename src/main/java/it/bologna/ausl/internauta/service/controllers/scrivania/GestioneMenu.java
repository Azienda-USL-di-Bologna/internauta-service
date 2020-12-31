package it.bologna.ausl.internauta.service.controllers.scrivania;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import it.bologna.ausl.blackbox.PermissionManager;
import it.bologna.ausl.blackbox.exceptions.BlackBoxPermissionException;
import it.bologna.ausl.internauta.service.authorization.AuthenticatedSessionData;
import it.bologna.ausl.internauta.service.authorization.AuthenticatedSessionDataBuilder;
import it.bologna.ausl.internauta.service.authorization.UserInfoService;
import it.bologna.ausl.internauta.service.exceptions.LambdaUncheckedException;
import it.bologna.ausl.internauta.service.repositories.baborg.UtenteRepository;
import it.bologna.ausl.internauta.service.repositories.scrivania.BmenuRepository;
import it.bologna.ausl.internauta.service.utils.InternautaConstants;
import it.bologna.ausl.internauta.service.utils.InternautaUtils;
import it.bologna.ausl.model.entities.baborg.Azienda;
import it.bologna.ausl.model.entities.baborg.Persona;
import it.bologna.ausl.model.entities.baborg.Ruolo;
import it.bologna.ausl.model.entities.baborg.Utente;
import it.bologna.ausl.model.entities.configuration.Applicazione;
import it.bologna.ausl.model.entities.scrivania.Bmenu;
import it.bologna.ausl.model.entities.scrivania.QBmenu;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import javax.persistence.Column;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

/**
 *
 * @author gusgus
 */
@Component
public class GestioneMenu {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(GestioneMenu.class);
    
    @Autowired
    UserInfoService userInfoService;
    
    @Autowired
    BmenuRepository bmenuRepository;
    
    @Autowired
    PermissionManager permissionManager;
    
    @Autowired 
    AuthenticatedSessionDataBuilder authenticatedSessionDataBuilder;
    
    @Autowired
    InternautaUtils internautaUtils;
    
    @Autowired
    ObjectMapper objectMapper;
    
    @Autowired
    UtenteRepository utenteRepository;
    
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
                .or(bmenu.modulo.isNotNull());
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

        Iterable<Bmenu> vociMenu = bmenuRepository.findAll(
                whereCondition, 
                Sort.by(Sort.Direction.ASC, bmenu.ordinale.getAnnotatedElement().getDeclaredAnnotation(Column.class).name()
        ));
        // Trasformo iterable in list
        List<Bmenu> vociMenuList = StreamSupport.stream(vociMenu.spliterator(), false).collect(Collectors.toList());
        
        vociMenuList.removeIf(voce -> {
            try {
                return !vocePermessa(voce, utente, permessiDiFlusso, ruoli);
            }
            catch (JsonProcessingException | BlackBoxPermissionException ex) {
                throw new LambdaUncheckedException("Errore nella rimozione voci delegate");
            }
        });
        return vociMenuList;
    }
    
    /**
     * Questa funzione risponde all'esigenza di rimuovere eventuali voci di menu
     * che avevano come unica buona condizione il fatto di essere collegate ad uno specifico modulo.
     * La voce è permessa quindi per una qualsiasi di queste condizioni:
     * 1- Ha il campo modulo null.
     * 2- Non servono permessi o ruoli.
     * 3- L'utente ha i permessi/ruoli.
     * 4- L'utente ha i ruoli nel modulo.
     * @param voce
     * @return 
     */
    private Boolean vocePermessa(Bmenu voce, Utente utente, List<String> permessiDiFlusso, List<String> ruoli) 
            throws JsonProcessingException, BlackBoxPermissionException {
        if (voce.getModulo() == null) {
            return true;
        }
        if (voce.getPermessiSufficienti() == null && voce.getRuoliSufficienti() == null) {
            return true;
        }
        if ((permessiDiFlusso != null && !Collections.disjoint(Arrays.asList(voce.getPermessiSufficienti()), permessiDiFlusso))
                || (ruoli != null && !Collections.disjoint(Arrays.asList(voce.getRuoliSufficienti()), ruoli))) {
            return true;
        }
        Map<String, List<Ruolo>> ruoliPerModuli = userInfoService.getRuoliPerModuli(utente, Boolean.TRUE);
        List<Ruolo> ruoliModulo = ruoliPerModuli.get(voce.getModulo());
        if (!ruoliModulo.isEmpty()) {
            return !Collections.disjoint(ruoliModulo, Arrays.asList(voce.getRuoliSufficienti()));
        }
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
    private ItemMenu buildaVoce(List<ItemMenu> menu, Bmenu bvoce, Utente utente) throws BlackBoxPermissionException, IOException {
        ItemMenu voceBuildata;
        
        // Filtro il menu alla ricerca dell'elemento con id = bvoce.id. La vocemenuList sarà vuota o conterrà un solo elemento
        List<ItemMenu> vocemenuList = menu.stream().filter(itemvoce -> itemvoce.getId().equals(bvoce.getId())).collect(Collectors.toList());
        
        if (vocemenuList.isEmpty()) {
            // La voce di menu ancora non è stata aggiunta. Quindi ora la buildo e la aggiungo.
            String url = null;
            Applicazione.UrlsGenerationStrategy urlGenerationStrategy = null;
            if (bvoce.getFoglia() && !bvoce.getScomponiPerAzienda() && bvoce.getCommandType().equals(Bmenu.CommandType.URL)) {
                url = buildaUrl(null, bvoce);
                urlGenerationStrategy = bvoce.getIdApplicazione().getUrlGenerationStrategy();
            }
            voceBuildata = new ItemMenu(bvoce.getId(), bvoce.getDescrizione(), url, bvoce.getIcona(), null, bvoce.getCommandType(), urlGenerationStrategy, bvoce.getScomponiPerAzienda(), bvoce.getFoglia());
            if (bvoce.getScomponiPerAzienda()) {
                // Aggiungo una sottovoce che è il link verso l'azienda dell'utente
                url = buildaUrl(utente.getIdAzienda(), bvoce);
                voceBuildata.setChildren(new ArrayList(Arrays.asList(
                    new ItemMenu(null, utente.getIdAzienda().getNome(), url, null, null, bvoce.getCommandType(), bvoce.getIdApplicazione().getUrlGenerationStrategy(), false, true)
                )));
            }
            menu.add(voceBuildata);
        } else {
            voceBuildata = vocemenuList.get(0);
            if (bvoce.getScomponiPerAzienda()) { // Se è da scomporre è sicuramente anche foglia eh...
                List<ItemMenu> children = voceBuildata.getChildren();
                String url = buildaUrl(utente.getIdAzienda(), bvoce);
                children.add(new ItemMenu(null, utente.getIdAzienda().getNome(), url, null, null, bvoce.getCommandType(), bvoce.getIdApplicazione().getUrlGenerationStrategy(), false, true));
            }
        }
        return voceBuildata;
    }
    
    /**
     * Ritorna l'url in Stringa che serve ad aprire la voce di menu.
     * @param azienda E' l'azienda target. Null se è una app internuata
     * @param bmenu Continene parte dell'url da buildare.
     * @return
     * @throws BlackBoxPermissionException
     * @throws IOException 
     */
    private String buildaUrl(Azienda azienda, Bmenu bmenu) throws BlackBoxPermissionException, IOException {
        AuthenticatedSessionData authenticatedSessionData = authenticatedSessionDataBuilder.getAuthenticatedUserProperties();
        Azienda aziendaLogin = authenticatedSessionData.getUser().getIdAzienda();
        Azienda aziendaTarget; 
        if (azienda != null) {
            aziendaTarget = azienda;
        } else {
            aziendaTarget = aziendaLogin;
        }
        String url = "";
        if(bmenu.getOpenCommand() != null && !bmenu.getOpenCommand().equals("")){
            url = bmenu.getOpenCommand();
        }
        return internautaUtils.getUrl(authenticatedSessionData, url, bmenu.getIdApplicazione().getId(), aziendaTarget);
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
            Utente utente) throws BlackBoxPermissionException, IOException {
        livello++;
        for (Bmenu voce : voci) {
            LOGGER.info("Buildo la voce " + voce.getDescrizione());
            ItemMenu menuPadre = buildaVoce(menu, voce, utente);
            
            if (!voce.getFoglia()) {
                LOGGER.info("Passo ai suoi figli");
                if (menuPadre.getChildren() == null) {
                    menuPadre.setChildren(new ArrayList());
                }
                List<ItemMenu> children = menuPadre.getChildren();
                List<Bmenu> sottobmenu = selectFromMenu(livello, voce.getId(), utente.getIdAzienda().getId(), permessiDiFlusso, ruoli, utente);
                cycleBuildAndDig(sottobmenu, livello, permessiDiFlusso, ruoli, children, utente);
            }
        }
    }
    
    /**
     * Gli scopi della funzione sono due. 
     * 1- Vengono eliminate le voci che dovrebbero avere dei figli e invece non ne hanno.
     * 2- Nelle voci che si dividono per azienda, se l'azienda è solo una allora la voce viene elimnata e l'opencommand spostato sul padre.
     * @param menu 
     */
    private void eliminaVociMorteAndScomposizioniUniche(List<ItemMenu> menu) {
        menu.removeIf(item -> {
           if (!item.getFoglia()) {
               List<ItemMenu> children = item.getChildren();
               if (children == null || children.isEmpty()) {
                   // E' una non foglia che non ha ricevuto neanche un figlio. Va eliminata.
                   return true;
               }
           }
           return false;
        });
        for (ItemMenu item : menu) {
            List<ItemMenu> children = item.getChildren();
            if (children != null && !children.isEmpty()) {
                if (item.getScomponiPerAzienda()) {
                    if (children.size() == 1) {
                        ItemMenu child = children.get(0);
                        item.setCommandType(child.getCommandType());
                        item.setOpenCommand(child.getOpenCommand());
                        item.setUrlGenerationStrategy(child.getUrlGenerationStrategy());
                        item.setChildren(null);
                    }
                } else {
                    eliminaVociMorteAndScomposizioniUniche(children);
                }
            }
        }
    }
    
    /**
     * Creazione del menu della scrivania internauta
     * @param persona 
     * @return  
     * @throws it.bologna.ausl.blackbox.exceptions.BlackBoxPermissionException 
     */
    // @Cacheable(value = "buildMenu__ribaltorg__", key = "{#persona.getId()}")
    public List<ItemMenu> buildMenu(Persona persona) throws BlackBoxPermissionException, IOException {
        LOGGER.info("Dentro la build menu. Carico un po' di roba.");
        List<Utente> utentiAttivi = userInfoService.getUtentiPersona(persona).stream().filter(u -> u.getAttivo()).collect(Collectors.toList());
        List<ItemMenu> menu = new ArrayList();
        
        // Variabili per filtrare i permessi sulla BlackBox
        List<String> ambitiFlusso = new ArrayList();
        ambitiFlusso.add(InternautaConstants.Permessi.Ambiti.PICO.toString());
        ambitiFlusso.add(InternautaConstants.Permessi.Ambiti.DETE.toString());
        ambitiFlusso.add(InternautaConstants.Permessi.Ambiti.DELI.toString());
        List<String> tipi = new ArrayList();
        tipi.add(InternautaConstants.Permessi.Tipi.FLUSSO.toString());
        
        for (Utente utente : utentiAttivi) {
            LOGGER.info("Lavoro sull'utente: " + utente.getId());
            
            // Prendo ruoli e permessi dell'utente. (Tra i ruoli ci sono anche gli interaziendali)
            Set<Ruolo> ruoliGenerali = userInfoService.getRuoliGenerali(utente, null);
            List<String> ruoli = ruoliGenerali.stream().map(ruolo -> ruolo.getNomeBreve().toString()).collect(Collectors.toList());
            List<String> permessiDiFlusso = permissionManager.getPermission(utente, ambitiFlusso, tipi);
            
            // Eseguo la Select del livello uno del menu
            LOGGER.info("Prima select");
            List<Bmenu> bmenu = selectFromMenu(1, null, utente.getIdAzienda().getId(), permessiDiFlusso, ruoli, utente);
            
            // Buildo le voci di menu trovate e le loro ramificazioni
            LOGGER.info("Inizio il building");
            cycleBuildAndDig(bmenu, 1, permessiDiFlusso, ruoli, menu, utente);
        }
        
        eliminaVociMorteAndScomposizioniUniche(menu);
        
        LOGGER.info("Menu completato");
        return menu;
    }
}

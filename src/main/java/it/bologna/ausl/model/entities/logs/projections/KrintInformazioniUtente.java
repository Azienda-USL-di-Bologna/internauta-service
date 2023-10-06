package it.bologna.ausl.model.entities.logs.projections;

import it.bologna.ausl.internauta.model.bds.types.PermessoEntitaStoredProcedure;
import it.bologna.ausl.model.entities.baborg.Ruolo;
import it.bologna.ausl.model.entities.baborg.Utente;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.rest.core.config.Projection;


@Projection(name = "KrintInformazioniUtente", types = Utente.class)
public interface KrintInformazioniUtente {
    
    @Value("#{target.getId()}") 
    Integer getId();
    
    @Value("#{target.getFax()}") 
    String getFax();
    
    @Value("#{target.getAttivo()}") 
    Boolean getAttivo();
    
    @Value("#{target.getEmails()}") 
    String[] getEmails();
    
    @Value("#{target.getVersion()}") 
    ZonedDateTime getVersion();
    
    @Value("#{target.getBitRuoli()}") 
    Integer getBitRuoli();
    
    @Value("#{target.getOmonimia()}") 
    Boolean getOmonimia();
    
    @Value("#{target.getTelefono()}") 
    String getTelefono();
    
    @Value("#{target.getUsername()}") 
    String getUsername(); 
    
    @Value("#{target.getIdAzienda().getId()}") 
    Integer getIdAzienda();
    
    // persona
    @Value("#{@userInfoService.getInformazioniPersonaKrint(target)}")
    KrintInformazioniPersona getIdPersona();
    
    @Value("#{target.getMappaRuoli()}") 
    Map<String, List<Ruolo>> getMappaRuoli();
    
    @Value("#{target.getIdInquadramento()}") 
    String getIdInquadramento();
    
    @Value("#{target.getPermessiDiFlusso()}") 
    List<PermessoEntitaStoredProcedure> getPermessiDiFlusso();
    
    @Value("#{target.getEntityDescription()}") 
    String getEntityDescription();
    
    @Value("#{target.getRuoliUtentiPersona()}") 
    Map<String, Map<String, List<String>>> getRuoliUtentiPersona();
    
    @Value("#{target.getStruttureDelSegretario()}") 
    Integer[] getStruttureDelSegretario();
    
    @Value("#{target.getPermessiDiFlussoByIdUtente()}") 
    List<PermessoEntitaStoredProcedure> getPermessiDiFlussoByIdUtente();
    
    @Value("#{target.getPermessiDiFlussoByCodiceAzienda()}") 
    Map<String, List<PermessoEntitaStoredProcedure>> getPermessiDiFlussoByCodiceAzienda();
}

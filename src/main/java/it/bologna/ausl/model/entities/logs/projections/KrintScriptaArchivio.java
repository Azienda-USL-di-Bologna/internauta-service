package it.bologna.ausl.model.entities.logs.projections;

import it.bologna.ausl.model.entities.scripta.Archivio;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.rest.core.config.Projection;

/**
 *
 * @author gusgus
 */
@Projection(name = "KrintScriptaArchivio", types = Archivio.class)
public interface KrintScriptaArchivio {

    @Value("#{target.getId()}")
    Integer getId();
    
    @Value("#{target.getTipo().toString()}")
    String getTipo();
    
    @Value("#{target.getRiservato() == true ? \"riservato\" : \"non riservato\"}")
    String getRiservato();
    
    @Value("#{target.getNumerazioneGerarchica()}")
    String getNumerazioneGerarchica();    

    @Value("#{target.getOggetto() == null || target.getOggetto().isEmpty() ? \"[Oggetto vuoto]\" : target.getOggetto() }")
    String getOggetto();
    
    @Value("#{target.getStato().toString().toLowerCase().replace(\"pre\", \"\")}")
    String getStato();
    
    @Value("#{target.getAnniTenuta() != null ? target.getAnniTenuta() == 999 ? \"illimitata\" : target.getAnniTenuta() + \" anni\" : null}")
    String getAnniTenuta();

    @Value("#{target.getIdTitolo()}")
    KrintScriptaTitolo getIdTitolo();
    
    @Value("#{target.getIdMassimario()}")
    KrintScriptaMassimario getMassimario();
    
    @Value("#{target.getNote()}")
    String getNote();
}

package it.bologna.ausl.model.entities.logs.projections;

import it.bologna.ausl.model.entities.rubrica.Contatto;
import it.bologna.ausl.model.entities.rubrica.DettaglioContatto;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.rest.core.config.Projection;

/**
 *
 * @author gusgus
 */
@Projection(name = "KrintRubricaGruppo", types = Contatto.class)
public interface KrintRubricaGruppo{
    
    Integer getId();
    String getDescrizione();
    String getCognome();
    String getNome();
    String getCodiceFiscale();
    String getRagioneSociale();
    String getPartitaIva();
    
    @Value("#{target.getTipo().name()}") 
    String getTipo();
    @Value("#{target.getCategoria().name()}") 
    String getCategoria();
    
    @Value("#{@projectionBeans.getCustomKrintContattiDelGruppoList(target)}")
    List<KrintRubricaGruppoContatto> getContattiDelGruppoList();

    
}

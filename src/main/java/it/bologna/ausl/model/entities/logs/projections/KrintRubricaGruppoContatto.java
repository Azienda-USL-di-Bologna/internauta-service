package it.bologna.ausl.model.entities.logs.projections;

import it.bologna.ausl.model.entities.rubrica.Contatto;
import it.bologna.ausl.model.entities.rubrica.DettaglioContatto;
import it.bologna.ausl.model.entities.rubrica.GruppiContatti;
import it.bologna.ausl.model.entities.shpeck.Draft;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.rest.core.config.Projection;

/**
 *
 * @author gusgus
 */
@Projection(name = "KrintRubricaGruppoContatto", types = GruppiContatti.class)
public interface KrintRubricaGruppoContatto{
    
    Integer getId();
    
    @Value("#{@projectionBeans.getCustomKrintContatto(target.getIdContatto())}")
    KrintRubricaContatto getIdContatto();
    
    @Value("#{@projectionBeans.getCustomKrintDettaglioContatto(target.getIdDettaglioContatto())}")
    KrintRubricaDettaglioContatto getIdDettaglioContatto();
}

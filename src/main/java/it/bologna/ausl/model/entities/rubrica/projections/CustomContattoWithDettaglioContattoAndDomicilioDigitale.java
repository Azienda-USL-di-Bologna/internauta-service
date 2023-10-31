package it.bologna.ausl.model.entities.rubrica.projections;

import it.bologna.ausl.model.entities.rubrica.Contatto;
import it.bologna.ausl.model.entities.rubrica.projections.generated.ContattoWithDettaglioContattoList;
import it.bologna.ausl.model.entities.rubrica.projections.generated.DettaglioContattoWithPlainFields;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.rest.core.config.Projection;

/**
 *
 * @author Top
 */
@Projection(name = "CustomContattoWithDettaglioContattoAndDomicilioDigitale", types = Contatto.class)
public interface CustomContattoWithDettaglioContattoAndDomicilioDigitale extends ContattoWithDettaglioContattoList {
    
    @Override
    @Value("#{@rubricaProjectionsUtils.getDettaglioContattoListWithDomicilioDigitale(target)}")
    public List<DettaglioContattoWithPlainFields> getDettaglioContattoList();
    
}

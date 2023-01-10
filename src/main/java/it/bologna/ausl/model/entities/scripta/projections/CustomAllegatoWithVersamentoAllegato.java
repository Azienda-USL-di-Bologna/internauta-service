package it.bologna.ausl.model.entities.scripta.projections;

import it.bologna.ausl.model.entities.scripta.Allegato;
import it.bologna.ausl.model.entities.scripta.projections.generated.AllegatoWithPlainFields;
import it.bologna.ausl.model.entities.versatore.VersamentoAllegato;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.rest.core.config.Projection;

/**
 *
 * @author gusgus
 */
@Projection(name = "CustomAllegatoWithVersamentoAllegato", types = Allegato.class)
public interface CustomAllegatoWithVersamentoAllegato extends AllegatoWithPlainFields {

    @Value("#{@scriptaProjectionUtils.getVersamentoAllegatoByIdVersamento("
        + "target, "
        + "@additionalDataParamsExtractor.getIdVersamento(),"
        + ")}")
    public VersamentoAllegato getVersamentoAllegato();
    
}

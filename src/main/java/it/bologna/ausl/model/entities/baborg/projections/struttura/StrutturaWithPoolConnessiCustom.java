package it.bologna.ausl.model.entities.baborg.projections.struttura;

import it.bologna.ausl.internauta.model.bds.types.PermessoEntitaStoredProcedure;
import it.bologna.ausl.model.entities.baborg.Struttura;
import it.bologna.ausl.model.entities.baborg.projections.generated.StrutturaWithPlainFields;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.rest.core.config.Projection;

/**
 *
 * @author gusgus
 */
@Projection(name = "StrutturaWithPoolConnessiCustom", types = Struttura.class)
public interface StrutturaWithPoolConnessiCustom extends StrutturaWithPlainFields {

    @Value("#{@userInfoService.getPermessiFilteredByAdditionalDataAndSetDescriptionOggetto("
        + "target,"
        + "@additionalDataParamsExtractor.getDataRiferimentoZoned(),"
        + "@additionalDataParamsExtractor.getModalita(),"
        + "@additionalDataParamsExtractor.getIdProvenienzaOggetto(),"
        + "@additionalDataParamsExtractor.getAmbitiPermesso(),"
        + "@additionalDataParamsExtractor.getTipiPermesso(),"
        + "@additionalDataParamsExtractor.getPredicatiPermesso(),"
        + "@additionalDataParamsExtractor.getDammiPermessiVirtualiPermesso(),"
        + "'tipologia'"
        + ")}")
    public List<PermessoEntitaStoredProcedure> getPermessiFilteredByAdditionalData();
    
}

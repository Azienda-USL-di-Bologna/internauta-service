package it.bologna.ausl.model.entities.baborg.projections;

import it.bologna.ausl.internauta.service.permessi.Permesso;
import it.bologna.ausl.model.entities.baborg.Utente;
import it.bologna.ausl.model.entities.baborg.projections.generated.UtenteWithIdPersona;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.rest.core.config.Projection;

/**
 *
 * @author Giuseppe Russo Pro
 */
@Projection(name = "UtenteWithIdPersonaAndPermessiByIdUtenteCustom", types = Utente.class)
public interface UtenteWithIdPersonaAndPermessiByIdUtenteCustom extends UtenteWithIdPersona {

//    @Override
//    @Value("#{@userInfoService.getPermessiDiFlussoByIdUtente("
//            + "target, "
//            + "@additionalDataParamsExtractor.getDataPermesso(), "
//            + "@additionalDataParamsExtractor.getEstraiStorico(),"
//            + "@additionalDataParamsExtractor.getIdProvenienzaOggetto())}")
//    public List<Permesso> getPermessiDiFlussoByIdUtente();
    
    @Value("#{@userInfoService.getPermessiFilteredByAdditionalDataByIdUtente("
        + "target, "
        + "@additionalDataParamsExtractor.getDataPermesso(),"
        + "@additionalDataParamsExtractor.getEstraiStorico(),"
        + "@additionalDataParamsExtractor.getIdProvenienzaOggetto(),"
        + "@additionalDataParamsExtractor.getAmbitiPermesso(),"
        + "@additionalDataParamsExtractor.getTipiPermesso(),"
        + ")}")
    public List<Permesso> getPermessiFilteredByAdditionalDataByIdUtente();
}

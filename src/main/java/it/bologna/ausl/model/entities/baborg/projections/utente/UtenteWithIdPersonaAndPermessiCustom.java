package it.bologna.ausl.model.entities.baborg.projections.utente;

import it.bologna.ausl.internauta.utils.bds.types.PermessoEntitaStoredProcedure;
import it.bologna.ausl.model.entities.baborg.Utente;
import it.bologna.ausl.model.entities.baborg.projections.generated.UtenteWithIdPersona;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;

import org.springframework.data.rest.core.config.Projection;

@Projection(name = "UtenteWithIdPersonaAndPermessiCustom", types = Utente.class)
public interface UtenteWithIdPersonaAndPermessiCustom extends UtenteWithIdPersona {

    @Value("#{@userInfoService.getPermessiFilteredByAdditionalData("
            + "target,"
            + "@additionalDataParamsExtractor.getDataRiferimentoZoned(),"
            + "@additionalDataParamsExtractor.getModalita(),"
            + "@additionalDataParamsExtractor.getIdProvenienzaOggetto(),"
            + "@additionalDataParamsExtractor.getAmbitiPermesso(),"
            + "@additionalDataParamsExtractor.getTipiPermesso(),"
            + "@additionalDataParamsExtractor.getPredicatiPermesso(),"
            + "@additionalDataParamsExtractor.getDammiPermessiVirtualiPermesso()"
            + ")}")
    public List<PermessoEntitaStoredProcedure> getPermessiFilteredByAdditionalData();

}

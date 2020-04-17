package it.bologna.ausl.model.entities.baborg.projections;

import it.bologna.ausl.internauta.utils.bds.types.PermessoEntitaStoredProcedure;
import it.bologna.ausl.model.entities.baborg.Utente;
import it.bologna.ausl.model.entities.baborg.projections.generated.UtenteWithIdPersona;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;

import org.springframework.data.rest.core.config.Projection;

@Projection(name = "UtenteWithIdPersonaAndPermessiCustom", types = Utente.class)
public interface UtenteWithIdPersonaAndPermessiCustom extends UtenteWithIdPersona {

    @Value("#{@userInfoService.getPermessiDiFlusso(target, @additionalDataParamsExtractor.getDataPermesso(), @additionalDataParamsExtractor.getEstraiStorico())}")
    @Override
    public List<PermessoEntitaStoredProcedure> getPermessiDiFlusso();

}

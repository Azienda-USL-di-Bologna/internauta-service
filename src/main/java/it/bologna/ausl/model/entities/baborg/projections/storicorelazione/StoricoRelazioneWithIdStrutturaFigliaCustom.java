package it.bologna.ausl.model.entities.baborg.projections.storicorelazione;

import it.bologna.ausl.model.entities.baborg.projections.struttura.StrutturaWithReplicheCustom;
import it.bologna.ausl.model.entities.baborg.StoricoRelazione;
import it.bologna.ausl.model.entities.baborg.projections.generated.StoricoRelazioneWithIdStrutturaFiglia;

import org.springframework.beans.factory.annotation.Value;

import org.springframework.data.rest.core.config.Projection;


@Projection(name = "StoricoRelazioneWithIdStrutturaFigliaCustom", types = StoricoRelazione.class)
public interface StoricoRelazioneWithIdStrutturaFigliaCustom extends StoricoRelazioneWithIdStrutturaFiglia {

    @Override
    @Value("#{@storicoRelazioneProjectionUtils.getStrutturaFigliaWithFogliaCalcolata(target,"
            + "@additionalDataParamsExtractor.getShowPool())}")
    public StrutturaWithReplicheCustom getIdStrutturaFiglia();

    
}

package it.bologna.ausl.model.entities.baborg.projections;

import it.bologna.ausl.model.entities.baborg.StoricoRelazione;
import it.bologna.ausl.model.entities.baborg.projections.generated.StoricoRelazioneWithIdStrutturaFiglia;
import it.bologna.ausl.model.entities.baborg.projections.generated.StrutturaWithPlainFields;

import org.springframework.beans.factory.annotation.Value;

import org.springframework.data.rest.core.config.Projection;


@Projection(name = "StoricoRelazioneWithIdStrutturaFigliaCustom", types = StoricoRelazione.class)
public interface StoricoRelazioneWithIdStrutturaFigliaCustom extends StoricoRelazioneWithIdStrutturaFiglia{

    @Override
//    @Value("#{@projectionsInterceptorLauncher.lanciaInterceptor(target, 'getIdStrutturaFiglia', 'StrutturaWithStruttureFiglieList')}")
    @Value("#{@projectionBeans.getStrutturaFigliaWithFogliaCalcolata(target)}")
    public StrutturaWithPlainFields getIdStrutturaFiglia();

}

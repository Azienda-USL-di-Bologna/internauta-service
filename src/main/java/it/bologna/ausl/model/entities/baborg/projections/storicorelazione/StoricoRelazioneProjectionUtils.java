/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.bologna.ausl.model.entities.baborg.projections.storicorelazione;

import com.querydsl.core.types.dsl.BooleanExpression;
import it.bologna.ausl.internauta.service.repositories.baborg.StoricoRelazioneRepository;
import it.bologna.ausl.internauta.service.utils.AdditionalDataParamsExtractor;
import it.bologna.ausl.model.entities.baborg.QStoricoRelazione;
import it.bologna.ausl.model.entities.baborg.StoricoRelazione;
import it.bologna.ausl.model.entities.baborg.Struttura;
import it.bologna.ausl.model.entities.baborg.projections.struttura.StrutturaWithReplicheCustom;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.stereotype.Component;

/**
 *
 * @author Top
 */

@Component
public class StoricoRelazioneProjectionUtils {
    
    @Autowired
    private AdditionalDataParamsExtractor additionalDataParamsExtractor;
    
    @Autowired
    protected StoricoRelazioneRepository storicoRelazioneRepository;
    
    @Autowired
    protected ProjectionFactory projectionFactory;
    
    public StrutturaWithReplicheCustom getStrutturaFigliaWithFogliaCalcolata(StoricoRelazione storicoRelazione, boolean showPool) {
        Struttura idStrutturaFiglia = storicoRelazione.getIdStrutturaFiglia();
        if (idStrutturaFiglia != null) {
            
            // Devo capire se questa struttura è una foglia.
            ZonedDateTime dataRiferimento = additionalDataParamsExtractor.getDataRiferimentoZoned().truncatedTo(ChronoUnit.DAYS);
            if (dataRiferimento == null) {
                dataRiferimento = ZonedDateTime.now().truncatedTo(ChronoUnit.DAYS);
            }
            QStoricoRelazione qStoricoRelazione = QStoricoRelazione.storicoRelazione;
            BooleanExpression filter = qStoricoRelazione.idStrutturaPadre.id.eq(idStrutturaFiglia.getId()).and(qStoricoRelazione.attivaDal.loe(dataRiferimento)
                    .and((qStoricoRelazione.attivaAl.isNull()).or(qStoricoRelazione.attivaAl.goe(dataRiferimento))));

            if (showPool == false) {
                filter = filter.and(qStoricoRelazione.idStrutturaFiglia.ufficio.eq(false));
            } 
            boolean isLeaf = !storicoRelazioneRepository.exists(filter);
            idStrutturaFiglia.setFogliaCalcolata(isLeaf);

            return projectionFactory.createProjection(StrutturaWithReplicheCustom.class, idStrutturaFiglia);
        }
        return null;
    }
}

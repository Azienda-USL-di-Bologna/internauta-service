/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.bologna.ausl.model.entities.permessi.projections;

import it.bologna.ausl.internauta.service.repositories.permessi.PredicatoAmbitoRepository;
import it.bologna.ausl.model.entities.permessi.PredicatoAmbito;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.stereotype.Component;

/**
 *
 * @author Top
 */
@Component
public class AmbitiSemanticiProjectionUtils {

    @Autowired
    protected ProjectionFactory projectionFactory;
    
    @Autowired
    protected PredicatoAmbitoRepository predicatoAmbitoRepository;

    public List<PredicatiAmbitiWithPredicatoAndPredicatiAmbitiImplicitiExpanded> expandPredicatiAmbiti(Integer[] idPredicatiAmbiti) {
        List<PredicatiAmbitiWithPredicatoAndPredicatiAmbitiImplicitiExpanded> res = new ArrayList();
        if (idPredicatiAmbiti != null) {
            for (Integer idPredicatoAmbito : idPredicatiAmbiti) {
                PredicatoAmbito predicatoAmbito = this.predicatoAmbitoRepository.getOne(idPredicatoAmbito);
                res.add(projectionFactory.createProjection(PredicatiAmbitiWithPredicatoAndPredicatiAmbitiImplicitiExpanded.class, predicatoAmbito));
            }
        }
        return res;
    }
}

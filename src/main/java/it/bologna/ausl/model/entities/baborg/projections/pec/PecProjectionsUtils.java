/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.bologna.ausl.model.entities.baborg.projections.pec;

import it.bologna.ausl.model.entities.logs.projections.KrintShpeckPec;
import it.bologna.ausl.model.entities.shpeck.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.stereotype.Component;

/**
 *
 * @author Top
 */
@Component
public class PecProjectionsUtils {
    
    @Autowired
    protected ProjectionFactory projectionFactory;
    
    public KrintShpeckPec getPecKrint(Message message) {
        return projectionFactory.createProjection(KrintShpeckPec.class, message.getIdPec());
    }
}

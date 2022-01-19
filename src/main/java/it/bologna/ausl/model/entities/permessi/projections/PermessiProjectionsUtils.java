/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.bologna.ausl.model.entities.permessi.projections;

import it.bologna.ausl.blackbox.repositories.EntitaRepository;
import it.bologna.ausl.blackbox.repositories.TipoEntitaRepository;
import it.bologna.ausl.internauta.service.repositories.permessi.PredicatoRepository;
import it.bologna.ausl.model.entities.permessi.Entita;
import it.bologna.ausl.model.entities.permessi.Permesso;
import it.bologna.ausl.model.entities.permessi.QEntita;
import it.bologna.ausl.model.entities.permessi.QTipoEntita;
import it.bologna.ausl.model.entities.permessi.TipoEntita;
import it.bologna.ausl.model.entities.permessi.projections.generated.EntitaWithPlainFields;
import it.nextsw.common.utils.EntityReflectionUtils;
import it.nextsw.common.utils.exceptions.EntityReflectionException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import javax.persistence.Table;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.stereotype.Component;

/**
 *
 * @author Top
 */
@Component
public class PermessiProjectionsUtils {
    
    @Autowired
    protected TipoEntitaRepository tipoEntitaRepository;
    
    @Autowired
    protected EntitaRepository entitaRepository;
    
    @Autowired
    protected ProjectionFactory projectionFactory;
    
    @Autowired
    protected PredicatoRepository predicatoRepository;
    
    public Object getEntita(Object object) throws EntityReflectionException, NoSuchMethodException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        Class entityClass = EntityReflectionUtils.getEntityFromProxyObject(object);
        Table tableAnnotation = (Table) entityClass.getAnnotation(Table.class);
        String targetSchema = tableAnnotation.schema();
        String targetTable = tableAnnotation.name();
        TipoEntita tipoEntita = tipoEntitaRepository.findOne(QTipoEntita.tipoEntita.targetSchema.eq(targetSchema)
                .and(QTipoEntita.tipoEntita.targetTable.eq(targetTable))).get();
        Method primaryKeyGetMethod = EntityReflectionUtils.getPrimaryKeyGetMethod(object);
        Object idEsterno = primaryKeyGetMethod.invoke(object);
        Entita entita = entitaRepository.findOne(
                QEntita.entita.idProvenienza.eq(Integer.parseInt(idEsterno.toString()))
                        .and(QEntita.entita.idTipoEntita.id.eq(tipoEntita.getId()))).get();
        return projectionFactory.createProjection(EntitaWithPlainFields.class, entita);
    }
    
    public Object getPredicato(Permesso permesso) {
        return predicatoRepository.findById(permesso.getIdPredicato().getId()).get();
    }
}

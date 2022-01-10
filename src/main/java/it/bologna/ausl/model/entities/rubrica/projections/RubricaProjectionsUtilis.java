package it.bologna.ausl.model.entities.rubrica.projections;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.stereotype.Component;

/**
 *
 * @author gusgus
 */
@Component
public class RubricaProjectionsUtilis {
    
    @Autowired
    protected ProjectionFactory factory;
    
    private static final Logger LOGGER = LoggerFactory.getLogger(RubricaProjectionsUtilis.class);
    
}

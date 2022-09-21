
package it.bologna.ausl.internauta.service.controllers.scripta;

import com.querydsl.core.types.dsl.BooleanExpression;
import it.bologna.ausl.internauta.service.repositories.scripta.PermessoArchivioRepository;
import it.bologna.ausl.model.entities.scripta.PermessoArchivio;
import it.bologna.ausl.model.entities.scripta.QPermessoArchivio;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 *
 * @author gusgus
 */
@Component
public class ScriptaArchiviUtils {
    
    @Autowired
    PermessoArchivioRepository permessoArchivioRepository;
    
    public boolean personHasAtLeastThisPermissionOnTheArchive(Integer idPersona, Integer idArchivio, PermessoArchivio.DecimalePredicato permesso) {
        QPermessoArchivio permessoArchivio = QPermessoArchivio.permessoArchivio;
        BooleanExpression filterUserhasPermission = permessoArchivio.idArchivioDetail.id.eq(idArchivio).and(
                permessoArchivio.idPersona.id.eq(idPersona).and(
                permessoArchivio.bit.goe(permesso.getValue()))
        );
        Optional<PermessoArchivio> findOne = permessoArchivioRepository.findOne(filterUserhasPermission);
        return findOne.isPresent();
    }
}

package it.bologna.ausl.internauta.service.repositories.scrivania;

import it.bologna.ausl.model.entities.scrivania.AutorizzatoreModificheDocumentali;
import it.bologna.ausl.model.entities.scrivania.QAutorizzatoreModificheDocumentali;
import it.bologna.ausl.model.entities.scrivania.projections.generated.AutorizzatoreModificheDocumentaliWithPlainFields;
import it.nextsw.common.annotations.NextSdrRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import it.nextsw.common.repositories.NextSdrQueryDslRepository;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * per convenzione nostra, collectionResourceRel e path devono avere lo stesso
 * nome tutto in minuscolo
 */
@NextSdrRepository(repositoryPath = "${scrivania.mapping.url.root}/autorizzatoremodifichedocumentali", defaultProjection = AutorizzatoreModificheDocumentaliWithPlainFields.class)
@RepositoryRestResource(collectionResourceRel = "autorizzatoremodifichedocumentali", path = "autorizzatoremodifichedocumentali", exported = false, excerptProjection = AutorizzatoreModificheDocumentaliWithPlainFields.class)
public interface AutorizzatoreModificheDocumentaliRepository extends
        NextSdrQueryDslRepository<AutorizzatoreModificheDocumentali, Integer, QAutorizzatoreModificheDocumentali>,
        JpaRepository<AutorizzatoreModificheDocumentali, Integer> {

}

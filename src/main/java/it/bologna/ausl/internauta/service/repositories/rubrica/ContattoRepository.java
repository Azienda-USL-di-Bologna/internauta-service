package it.bologna.ausl.internauta.service.repositories.rubrica;

import it.bologna.ausl.model.entities.rubrica.Contatto;
import it.bologna.ausl.model.entities.rubrica.QContatto;
import it.bologna.ausl.model.entities.rubrica.projections.generated.ContattoWithPlainFields;
import it.nextsw.common.annotations.NextSdrRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import it.nextsw.common.repositories.NextSdrQueryDslRepository;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * per convenzione nostra, collectionResourceRel e path devono avere lo stesso
 * nome tutto in minuscolo
 */
@NextSdrRepository(repositoryPath = "${rubrica.mapping.url.root}/contatto", defaultProjection = ContattoWithPlainFields.class)
@RepositoryRestResource(collectionResourceRel = "contatto", path = "contatto", exported = false, excerptProjection = ContattoWithPlainFields.class)
public interface ContattoRepository extends
        NextSdrQueryDslRepository<Contatto, Integer, QContatto>,
        JpaRepository<Contatto, Integer> {

}

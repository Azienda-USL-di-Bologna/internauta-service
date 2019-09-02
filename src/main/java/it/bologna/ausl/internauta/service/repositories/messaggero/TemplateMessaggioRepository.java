package it.bologna.ausl.internauta.service.repositories.messaggero;

import it.bologna.ausl.model.entities.messaggero.QTemplateMessaggio;
import it.bologna.ausl.model.entities.messaggero.TemplateMessaggio;
import it.bologna.ausl.model.entities.messaggero.projections.generated.TemplateMessaggioWithPlainFields;
import it.nextsw.common.annotations.NextSdrRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import it.nextsw.common.repositories.NextSdrQueryDslRepository;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 *
 * @author Giuseppe Russo <g.russo@nsi.it>
 */
@NextSdrRepository(repositoryPath = "${messaggero.mapping.url.root}/templatemessaggio", defaultProjection = TemplateMessaggioWithPlainFields.class)
@RepositoryRestResource(collectionResourceRel = "templatemessaggio", path = "templatemessaggio", exported = false, excerptProjection = TemplateMessaggioWithPlainFields.class)
public interface TemplateMessaggioRepository extends
        NextSdrQueryDslRepository<TemplateMessaggio, Integer, QTemplateMessaggio>, 
        JpaRepository<TemplateMessaggio, Integer> {
}

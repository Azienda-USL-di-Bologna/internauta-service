package it.bologna.ausl.internauta.service.repositories.diagnostica;

import it.bologna.ausl.model.entities.diagnostica.QReport;
import it.bologna.ausl.model.entities.diagnostica.Report;
import it.bologna.ausl.model.entities.diagnostica.projections.generated.ReportWithPlainFields;
import it.nextsw.common.data.annotations.NextSdrRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import it.nextsw.common.repositories.NextSdrQueryDslRepository;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * per convenzione nostra, collectionResourceRel e path devono avere lo stesso
 * nome tutto in minuscolo
 */
@NextSdrRepository(repositoryPath = "${diagnostica.mapping.url.root}/report", defaultProjection = ReportWithPlainFields.class)
@RepositoryRestResource(collectionResourceRel = "report", path = "report", exported = false, excerptProjection = ReportWithPlainFields.class)
public interface ReportRepository extends
        NextSdrQueryDslRepository<Report, String, QReport>,
        JpaRepository<Report, String> {
}

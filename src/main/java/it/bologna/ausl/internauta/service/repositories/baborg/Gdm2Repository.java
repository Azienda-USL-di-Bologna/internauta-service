package it.bologna.ausl.internauta.service.repositories.baborg;


import it.bologna.ausl.model.entities.baborg.Gdm2;
import it.bologna.ausl.model.entities.baborg.QGdm2;
import it.bologna.ausl.model.entities.baborg.projections.generated.Gdm2WithPlainFields;
import it.nextsw.common.annotations.NextSdrRepository;
import it.nextsw.common.repositories.NextSdrQueryDslRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@NextSdrRepository(repositoryPath = "${baborg.mapping.url.root}/gdm2", defaultProjection = Gdm2WithPlainFields.class)
@RepositoryRestResource(collectionResourceRel = "gdm2", path = "gdm2", exported = false)
public interface Gdm2Repository extends
        NextSdrQueryDslRepository<Gdm2, Integer, QGdm2>,
        JpaRepository<Gdm2, Integer> {
}

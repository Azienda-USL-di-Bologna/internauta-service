package it.bologna.ausl.internauta.service.repositories.rubrica;

import it.bologna.ausl.model.entities.rubrica.Contatto;
import it.bologna.ausl.model.entities.rubrica.DettaglioContatto;
import it.bologna.ausl.model.entities.rubrica.QDettaglioContatto;
import it.bologna.ausl.model.entities.rubrica.projections.generated.DettaglioContattoWithPlainFields;
import it.nextsw.common.annotations.NextSdrRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import it.nextsw.common.repositories.NextSdrQueryDslRepository;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.query.Procedure;
import org.springframework.data.repository.query.Param;

/**
 * per convenzione nostra, collectionResourceRel e path devono avere lo stesso
 * nome tutto in minuscolo
 */
@NextSdrRepository(repositoryPath = "${rubrica.mapping.url.root}/dettagliocontatto", defaultProjection = DettaglioContattoWithPlainFields.class)
@RepositoryRestResource(collectionResourceRel = "dettagliocontatto", path = "dettagliocontatto", exported = false, excerptProjection = DettaglioContattoWithPlainFields.class)
public interface DettaglioContattoRepository extends
        NextSdrQueryDslRepository<DettaglioContatto, Integer, QDettaglioContatto>,
        JpaRepository<DettaglioContatto, Integer> {

    @Procedure("rubrica.get_similar_contacts")
    public String getSimilarContacts(
            @Param("contact") String contact
    );

    public DettaglioContatto findByIdContattoAndIdContattoEsterno(Contatto idContatto, Contatto idContattoEsterno);

    public DettaglioContatto findByIdContattoAndTipo(Contatto idContatto, String tipo);

    public List<DettaglioContatto> findByIdContatto(Contatto idContatto);
}

package it.bologna.ausl.internauta.service.repositories.rubrica;

import it.bologna.ausl.model.entities.rubrica.Contatto;
import it.bologna.ausl.model.entities.rubrica.QContatto;
import it.bologna.ausl.model.entities.rubrica.projections.generated.ContattoWithPlainFields;
import it.nextsw.common.annotations.NextSdrRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import it.nextsw.common.repositories.NextSdrQueryDslRepository;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.query.Procedure;
import org.springframework.data.repository.query.Param;

/**
 * per convenzione nostra, collectionResourceRel e path devono avere lo stesso
 * nome tutto in minuscolo
 */
@NextSdrRepository(repositoryPath = "${rubrica.mapping.url.root}/contatto", defaultProjection = ContattoWithPlainFields.class)
@RepositoryRestResource(collectionResourceRel = "contatto", path = "contatto", exported = false, excerptProjection = ContattoWithPlainFields.class)
public interface ContattoRepository extends
        NextSdrQueryDslRepository<Contatto, Integer, QContatto>,
        JpaRepository<Contatto, Integer> {

    @Procedure("rubrica.get_similar_contacts")
    public String getSimilarContacts(
            @Param("contact") String contact,
            @Param("p_id_aziende") String idAziende
    );

    @Value("select * from rubrica.contatti where id_esterno = ?1 and categoria = ?2")
    public List<Contatto> findByIdEsternoAndCategoria(String idEsterno, String categoria);
}

package it.bologna.ausl.internauta.service.repositories.configurazione;

import it.bologna.ausl.model.entities.configurazione.ImpostazioniApplicazioni;
import it.bologna.ausl.model.entities.configurazione.QImpostazioniApplicazioni;
import it.bologna.ausl.model.entities.configurazione.projections.generated.ImpostazioniApplicazioniWithPlainFields;
import it.nextsw.common.annotations.NextSdrRepository;
import it.nextsw.common.repositories.NextSdrQueryDslRepository;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

/**
 *
 * @author gdm
 */
@NextSdrRepository(repositoryPath = "${configurazione.mapping.url.root}/impostazioniapplicazioni", defaultProjection = ImpostazioniApplicazioniWithPlainFields.class)
@RepositoryRestResource(collectionResourceRel = "impostazioniapplicazioni", path = "impostazioniapplicazioni", exported = false, excerptProjection = ImpostazioniApplicazioniWithPlainFields.class)
public interface ImpostazioniApplicazioniRepository extends
        NextSdrQueryDslRepository<ImpostazioniApplicazioni, String, QImpostazioniApplicazioni>,
        JpaRepository<ImpostazioniApplicazioni, String> {

    @Query(value = "select * from  configurazione.impostazioni_applicazioni\n"
            + "ia where cast(impostazioni_visualizzazione as text) ilike '%scrivania.emailToNotify%'  and "
            + " id_applicazione = 'scrivania' ", nativeQuery = true)
    public List<ImpostazioniApplicazioni> getEmailToNotify();
}

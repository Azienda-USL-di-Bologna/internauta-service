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

    @Query(value = "select i.id, i.id_persona, i.id_applicazione,"
            + " i.impostazioni_visualizzazione,"
            + " i.version from  configurazione.impostazioni_applicazioni i"
            + " inner join baborg.persone p on i.id_persona = p.id \n"
            + "where cast(i.impostazioni_visualizzazione as text) ilike"
            + " '%scrivania.emailToNotify%' and p.id_azienda_default = ?1", nativeQuery = true)
    public List<ImpostazioniApplicazioni> getEmailToNotify(Integer azienda);
}

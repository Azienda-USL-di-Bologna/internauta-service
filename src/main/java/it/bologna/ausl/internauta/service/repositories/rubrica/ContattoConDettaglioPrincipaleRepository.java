package it.bologna.ausl.internauta.service.repositories.rubrica;

import it.bologna.ausl.model.entities.rubrica.Contatto;
import it.bologna.ausl.model.entities.rubrica.views.ContattoConDettaglioPrincipale;
import it.bologna.ausl.model.entities.rubrica.views.QContattoConDettaglioPrincipale;
import it.bologna.ausl.model.entities.rubrica.views.projections.generated.ContattoConDettaglioPrincipaleWithPlainFields;
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
@NextSdrRepository(repositoryPath = "${rubrica.mapping.url.root}/contattocondettaglioprincipale", defaultProjection = ContattoConDettaglioPrincipaleWithPlainFields.class)
@RepositoryRestResource(collectionResourceRel = "contattocondettaglioprincipale", path = "contattocondettaglioprincipale", exported = false, excerptProjection = ContattoConDettaglioPrincipaleWithPlainFields.class)
public interface ContattoConDettaglioPrincipaleRepository extends
        NextSdrQueryDslRepository<ContattoConDettaglioPrincipale, Integer, QContattoConDettaglioPrincipale>,
        JpaRepository<ContattoConDettaglioPrincipale, Integer> {

    @Procedure("rubrica.get_similar_contacts")
    public String getSimilarContacts(
            @Param("contact") String contact,
            @Param("p_id_aziende") String idAziende
    );

    @Value("select * from rubrica.contatti where id_esterno = ?1 and categoria = ?2")
    public List<Contatto> findByIdEsternoAndCategoria(String idEsterno, String categoria);
    
    @Value("select * from rubrica.contatti where codice_fiscale = ?1")
    public List<Contatto> findByCodiceFiscale(String cf);
    
    @Value("select * from rubrica.contatti where partita_iva = ?1")
    public List<Contatto> findByPartitaIva(String piva);
}

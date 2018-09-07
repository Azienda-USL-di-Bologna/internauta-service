//package it.bologna.ausl.baborg.service.repositories;
//
//import it.bologna.ausl.model.entities.configuration.Applicazione;
//import it.bologna.ausl.model.entities.configuration.QApplicazione;
//import it.bologna.ausl.model.entities.configuration.projections.generated.ApplicazioneWithPlainFields;
//import it.nextsw.common.annotations.NextSdrRepository;
//import org.springframework.data.rest.core.annotation.RepositoryRestResource;
//import it.nextsw.common.repositories.NextSdrQueryDslRepository;
//import org.springframework.data.jpa.repository.JpaRepository;
//
///**
// * per convenzione nostra, collectionResourceRel e path devono avere lo stesso
// * nome tutto in minuscolo.
// *
// * JpaRepository: permette di fare le operazioni base (insert, delete, update,
// * select)
// *
// * NextSdrQueryDslRepository: serve per fare le query con gli oggetti Q
// *
// *
// * exported: definisce se si esporta il repository. Nel nostro framework si
// * passa dal controller e quindi deve essere semrpe settato a false.
// *
// *
// * excerptProjection: projection di default. Se si chiamano le cose senza
// * projection, è valida quella indicata.
// *
// */
//@NextSdrRepository(repositoryPath = "applicazione", defaultProjection = ApplicazioneWithPlainFields.class)
//@RepositoryRestResource(collectionResourceRel = "applicazione", path = "applicazione", exported = false)
//public interface ApplicazioneRepository extends
//        NextSdrQueryDslRepository<Applicazione, Integer, QApplicazione>,
//        JpaRepository<Applicazione, String> {
//}

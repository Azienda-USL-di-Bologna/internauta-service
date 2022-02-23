//package it.bologna.ausl.internauta.service.repositories.scripta;
//
//import it.bologna.ausl.model.entities.scripta.DettaglioAllegato;
//import it.bologna.ausl.model.entities.scripta.QDettaglioAllegato;
//import it.bologna.ausl.model.entities.scripta.projections.generated.DettaglioAllegatoWithPlainFields;
//import it.nextsw.common.annotations.NextSdrRepository;
//import it.nextsw.common.repositories.NextSdrQueryDslRepository;
//import org.springframework.data.jpa.repository.JpaRepository;
//import org.springframework.data.rest.core.annotation.RepositoryRestResource;
//
///**
// *
// * @author Mdor
// */
//@NextSdrRepository(repositoryPath = "${scripta.mapping.url.root}/dettaglioallegato", defaultProjection = DettaglioAllegatoWithPlainFields.class)
//@RepositoryRestResource(collectionResourceRel = "dettaglioallegato", path = "dettaglioallegato", exported = false, excerptProjection = DettaglioAllegatoWithPlainFields.class)
//public interface DettaglioAllegatoRepository extends
//        NextSdrQueryDslRepository<DettaglioAllegato, Integer, QDettaglioAllegato>,
//        JpaRepository<DettaglioAllegato, Integer> {
//}
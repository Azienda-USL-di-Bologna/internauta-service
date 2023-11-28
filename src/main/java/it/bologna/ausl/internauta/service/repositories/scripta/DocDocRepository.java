/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package it.bologna.ausl.internauta.service.repositories.scripta;

import it.bologna.ausl.model.entities.scripta.Doc;
import it.bologna.ausl.model.entities.scripta.DocDoc;
import it.bologna.ausl.model.entities.scripta.QDocDoc;
import it.bologna.ausl.model.entities.scripta.projections.generated.DocDocWithPlainFields;
import it.nextsw.common.data.annotations.NextSdrRepository;
import it.nextsw.common.repositories.NextSdrQueryDslRepository;
import java.util.ArrayList;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

/**
 *
 * @author chiaralazz
 */
@NextSdrRepository(repositoryPath = "${scripta.mapping.url.root}/docdoc", defaultProjection = DocDocWithPlainFields.class)
@RepositoryRestResource(collectionResourceRel = "docdoc", path = "docdoc", exported = false, excerptProjection = DocDocWithPlainFields.class)
public interface DocDocRepository extends
        NextSdrQueryDslRepository<DocDoc, Integer, QDocDoc>,
        JpaRepository<DocDoc, Integer>{
    public ArrayList<DocDoc> getByIdDocSorgente(Doc idDocSorgente);
}

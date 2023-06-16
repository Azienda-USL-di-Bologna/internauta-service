/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package it.bologna.ausl.internauta.service.repositories.scripta;

import it.bologna.ausl.model.entities.scripta.Doc;
import it.bologna.ausl.model.entities.scripta.NoteVersamento;
import it.bologna.ausl.model.entities.scripta.QNoteVersamento;
import it.bologna.ausl.model.entities.scripta.projections.generated.NoteVersamentoWithPlainFields;
import it.nextsw.common.annotations.NextSdrRepository;
import it.nextsw.common.repositories.NextSdrQueryDslRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

/**
 *
 * @author utente
 */
@NextSdrRepository(repositoryPath = "${scripta.mapping.url.root}/noteversamento", defaultProjection = NoteVersamentoWithPlainFields.class)
@RepositoryRestResource(collectionResourceRel = "noteversamento", path = "noteversamento", exported = false, excerptProjection = NoteVersamentoWithPlainFields.class)
public interface NoteVersamentoRepository extends
        NextSdrQueryDslRepository<NoteVersamento, Integer, QNoteVersamento>,
        JpaRepository<NoteVersamento, Integer> {
    public NoteVersamento findByIdDoc(Integer idDoc);
    
}

package it.bologna.ausl.internauta.service.repositories.shpeck;

import it.bologna.ausl.model.entities.shpeck.Note;
import it.bologna.ausl.model.entities.shpeck.QNote;
import it.bologna.ausl.model.entities.shpeck.projections.generated.NoteWithPlainFields;
import it.nextsw.common.annotations.NextSdrRepository;
import it.nextsw.common.repositories.NextSdrQueryDslRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

/**
 *
 * @author Giuseppe Russo <g.russo@nsi.it>
 */
@NextSdrRepository(repositoryPath = "${shpeck.mapping.url.root}/note", defaultProjection = NoteWithPlainFields.class)
@RepositoryRestResource(collectionResourceRel = "note", path = "note", exported = false, excerptProjection = NoteWithPlainFields.class)
public interface NoteRepository extends 
        NextSdrQueryDslRepository<Note, Integer, QNote>,
        JpaRepository<Note, Integer>{
    
}

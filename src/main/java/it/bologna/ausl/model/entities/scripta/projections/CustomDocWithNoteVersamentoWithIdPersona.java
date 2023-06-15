
/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package it.bologna.ausl.model.entities.scripta.projections;

import it.bologna.ausl.model.entities.scripta.Doc;
import it.bologna.ausl.model.entities.scripta.projections.archivio.CustomArchivioDocWithIdArchivioAndIdPersonaArchiviazione;
import it.bologna.ausl.model.entities.scripta.projections.generated.NoteVersamentoWithIdPersona;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.rest.core.config.Projection;

/**
 *
 * @author utente
 */
@Projection(name = "CustomDocWithNoteVersamentoWithIdPersona", types = Doc.class)
public interface CustomDocWithNoteVersamentoWithIdPersona {
    @Value("#{@projectionsInterceptorLauncher.lanciaInterceptorCollection(target, 'getNoteVersamentoList', 'NoteVersamentoWithIdPersona')}")
    public List<NoteVersamentoWithIdPersona> getNoteVersamentoList();
    
    
}

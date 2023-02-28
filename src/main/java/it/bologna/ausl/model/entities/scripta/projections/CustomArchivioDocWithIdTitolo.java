package it.bologna.ausl.model.entities.scripta.projections;

import com.fasterxml.jackson.annotation.JsonFormat;

import it.bologna.ausl.model.entities.scripta.ArchivioDoc;

import org.springframework.beans.factory.annotation.Value;

import org.springframework.data.rest.core.config.Projection;


@Projection(name = "CustomArchivioDocWithIdTitolo", types = it.bologna.ausl.model.entities.scripta.ArchivioDoc.class)
public interface CustomArchivioDocWithIdTitolo extends it.bologna.ausl.model.entities.scripta.projections.generated.ArchivioDocWithIdArchivioAndIdPersonaArchiviazione {
    @Value("#{@projectionsInterceptorLauncher.lanciaInterceptor(target, 'getIdArchivio', 'ArchivioWithIdTitolo')}")
    public Object getIdArchivio();
}

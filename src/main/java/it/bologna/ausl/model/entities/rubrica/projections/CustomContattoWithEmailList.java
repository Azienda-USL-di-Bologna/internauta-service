package it.bologna.ausl.model.entities.rubrica.projections;

import it.bologna.ausl.model.entities.rubrica.projections.generated.ContattoWithEmailList;

import org.springframework.beans.factory.annotation.Value;

import org.springframework.data.rest.core.config.Projection;

@Projection(name = "CustomContattoWithEmailList", types = it.bologna.ausl.model.entities.rubrica.Contatto.class)
public interface CustomContattoWithEmailList extends ContattoWithEmailList {

    public Object getEmailList();
}

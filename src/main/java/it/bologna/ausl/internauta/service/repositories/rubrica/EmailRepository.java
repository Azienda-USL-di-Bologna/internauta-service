/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package it.bologna.ausl.internauta.service.repositories.rubrica;

import it.bologna.ausl.model.entities.rubrica.Email;
import it.bologna.ausl.model.entities.rubrica.QEmail;
import it.bologna.ausl.model.entities.rubrica.projections.generated.EmailWithPlainFields;
import it.nextsw.common.annotations.NextSdrRepository;
import it.nextsw.common.repositories.NextSdrQueryDslRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

/**
 *
 * @author utente
 */
@NextSdrRepository(repositoryPath = "${rubrica.mapping.url.root}/email", defaultProjection = EmailWithPlainFields.class)
@RepositoryRestResource(collectionResourceRel = "email", path = "email", exported = false, excerptProjection = EmailWithPlainFields.class)
public interface EmailRepository extends
        NextSdrQueryDslRepository<Email, Integer, QEmail>,
        JpaRepository<Email, Integer> {
    
}

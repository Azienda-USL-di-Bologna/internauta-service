/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.bologna.ausl.internauta.service.repositories.scrivania;

import it.bologna.ausl.model.entities.scrivania.Menu;
import it.bologna.ausl.model.entities.scrivania.QMenu;
import it.bologna.ausl.model.entities.scrivania.projections.generated.MenuWithPlainFields;
import it.nextsw.common.annotations.NextSdrRepository;
import it.nextsw.common.repositories.NextSdrQueryDslRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

/**
 *
 * @author fayssel
 */
@NextSdrRepository(repositoryPath = "${scrivania.mapping.url.root}/menu", defaultProjection = MenuWithPlainFields.class)
@RepositoryRestResource(collectionResourceRel = "menu", path = "attivita", exported = false, excerptProjection = MenuWithPlainFields.class)
public interface MenuRepository extends 
        NextSdrQueryDslRepository<Menu, Integer, QMenu>,
        JpaRepository<Menu, Integer>{
    
}

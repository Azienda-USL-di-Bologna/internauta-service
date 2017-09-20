package it.bologna.ausl.baborg.service;

import it.bologna.ausl.entities.baborg.Utente;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

/**
 * Created by user on 14/06/2017.
 */


public interface UtenteRepository extends CrudRepository<Utente,Integer>{

    public Utente findByUsername(@Param("username") String username);
}

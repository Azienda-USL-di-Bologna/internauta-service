/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.bologna.ausl.baborg.service.repositories;

import it.bologna.ausl.baborg.model.entities.RibaltoniDaLanciare;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.query.Procedure;

/**
 *
 * @author fayssel
 */
public interface RibaltoniDaLanciareRepository extends JpaRepository<RibaltoniDaLanciare, Integer>{
    @Procedure("ribaltone_utils.inserisci_ribaltone_da_lanciare")
    void inserisciRibaltoneDaLanciare(String codiceAzienda, String indirizzoMail, Integer idUtente);
}

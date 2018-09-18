package it.bologna.ausl.internauta.service.repositories.baborg;

import it.bologna.ausl.model.entities.baborg.RibaltoniDaLanciare;
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

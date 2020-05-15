package it.bologna.ausl.internauta.service.permessi;

import it.bologna.ausl.model.entities.baborg.Struttura;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 *
 * @author Giuseppe Russo
 */
public class Permesso implements Comparable<Permesso>, Serializable{
 
    private String ambito;
    private String permesso;
    private LocalDateTime attivoDal;
    private LocalDateTime attivoAl;
    private String nomeStruttura;
    private Boolean attivo;

    public String getAmbito() {
        return ambito;
    }

    public void setAmbito(String ambito) {
        this.ambito = ambito;
    }

    public String getPermesso() {
        return permesso;
    }

    public void setPermesso(String permesso) {
        this.permesso = permesso;
    }

    public LocalDateTime getAttivoDal() {
        return attivoDal;
    }

    public void setAttivoDal(LocalDateTime attivoDal) {
        this.attivoDal = attivoDal;
    }

    public LocalDateTime getAttivoAl() {
        return attivoAl;
    }

    public void setAttivoAl(LocalDateTime attivoAl) {
        this.attivoAl = attivoAl;
    }

    public String getNomeStruttura() {
        return nomeStruttura;
    }

    public void setNomeStruttura(String nomeStruttura) {
        this.nomeStruttura = nomeStruttura;
    }

    public Boolean getAttivo() {
        return attivo;
    }

    public void setAttivo(Boolean attivo) {
        this.attivo = attivo;
    }

    @Override
    public int compareTo(Permesso o) {
        int compareStruttura = this.getNomeStruttura().compareTo(o.getNomeStruttura());
        if (compareStruttura == 0) {
            int compareAmbito = this.getAmbito().compareTo(o.getAmbito());
            if (compareAmbito == 0) {
                int comparePermesso = this.getPermesso().compareTo(o.getPermesso());
                if (comparePermesso == 0) {
                    return this.getAttivoDal().compareTo(o.getAttivoDal());
                } else {
                    return comparePermesso;
                }
            } else {
                return compareAmbito;
            }
        } else {
           return compareStruttura;
        }        
    }
            
}

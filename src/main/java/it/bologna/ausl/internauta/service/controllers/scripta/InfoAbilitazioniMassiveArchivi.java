package it.bologna.ausl.internauta.service.controllers.scripta;

import java.util.List;

/**
 *
 * @author gusgus
 */
public class InfoAbilitazioniMassiveArchivi {
    private List<Integer> idPersonaVicariDaAggiungere;
    private List<Integer> idPersonaVicariDaRimuovere;
    private List<PermessoPersona> permessiPersonaDaAggiungere;
    private List<Integer> idPersonaPermessiDaRimuovere;

    public InfoAbilitazioniMassiveArchivi() {
    }

    public List<Integer> getIdPersonaVicariDaAggiungere() {
        return idPersonaVicariDaAggiungere;
    }

    public void setIdPersonaVicariDaAggiungere(List<Integer> idPersonaVicariDaAggiungere) {
        this.idPersonaVicariDaAggiungere = idPersonaVicariDaAggiungere;
    }

    public List<Integer> getIdPersonaVicariDaRimuovere() {
        return idPersonaVicariDaRimuovere;
    }

    public void setIdPersonaVicariDaRimuovere(List<Integer> idPersonaVicariDaRimuovere) {
        this.idPersonaVicariDaRimuovere = idPersonaVicariDaRimuovere;
    }

    public List<PermessoPersona> getPermessiPersonaDaAggiungere() {
        return permessiPersonaDaAggiungere;
    }

    public void setPermessiPersonaDaAggiungere(List<PermessoPersona> permessiPersonaDaAggiungere) {
        this.permessiPersonaDaAggiungere = permessiPersonaDaAggiungere;
    }

    public List<Integer> getIdPersonaPermessiDaRimuovere() {
        return idPersonaPermessiDaRimuovere;
    }

    public void setIdPersonaPermessiDaRimuovere(List<Integer> idPersonaPermessiDaRimuovere) {
        this.idPersonaPermessiDaRimuovere = idPersonaPermessiDaRimuovere;
    }
    
    public class PermessoPersona {
        private Integer idPersona;
        private String permesso;
        private Integer idStruttura;

        public PermessoPersona() {
        }

        public Integer getIdPersona() {
            return idPersona;
        }

        public void setIdPersona(Integer idPersona) {
            this.idPersona = idPersona;
        }

        public String getPermesso() {
            return permesso;
        }

        public void setPermesso(String permesso) {
            this.permesso = permesso;
        }

        public Integer getIdStruttura() {
            return idStruttura;
        }

        public void setIdStruttura(Integer idStruttura) {
            this.idStruttura = idStruttura;
        }
    }
}

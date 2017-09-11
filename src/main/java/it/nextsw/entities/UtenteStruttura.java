/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.nextsw.entities;

import it.nextsw.olingo.interceptor.queryextension.BaseOdataJpaQueryExtension;
import java.io.Serializable;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 * @author Giovanni
 */
@Entity
@EntityListeners(BaseOdataJpaQueryExtension.class)
@Table(name = "utente_struttura", catalog = "babel", schema = "organigramma")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "UtenteStruttura.findAll", query = "SELECT u FROM UtenteStruttura u")
    , @NamedQuery(name = "UtenteStruttura.findById", query = "SELECT u FROM UtenteStruttura u WHERE u.id = :id")})
public class UtenteStruttura implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @Column(name = "id")
    private Integer id;
    @JoinColumn(name = "id_afferenza_struttura", referencedColumnName = "id")
    @ManyToOne(optional = false)
    private AfferenzaStruttura idAfferenzaStruttura;
    @JoinColumn(name = "id_struttura", referencedColumnName = "id")
    @ManyToOne(optional = false)
    private Struttura idStruttura;

    @JoinColumn(name = "id_utente", referencedColumnName = "id")
    @ManyToOne(optional = false)
    private Utente idUtente;

    public UtenteStruttura() {
    }

    public UtenteStruttura(Integer id) {
        this.id = id;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public AfferenzaStruttura getIdAfferenzaStruttura() {
        return idAfferenzaStruttura;
    }

    public void setIdAfferenzaStruttura(AfferenzaStruttura idAfferenzaStruttura) {
        this.idAfferenzaStruttura = idAfferenzaStruttura;
    }

    public Struttura getIdStruttura() {
        return idStruttura;
    }

    public void setIdStruttura(Struttura idStruttura) {
        this.idStruttura = idStruttura;
    }

    public Utente getIdUtente() {
        return idUtente;
    }

    public void setIdUtente(Utente idUtente) {
        this.idUtente = idUtente;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (id != null ? id.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof UtenteStruttura)) {
            return false;
        }
        UtenteStruttura other = (UtenteStruttura) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "it.nextsw.entities.UtenteStruttura[ id=" + id + " ]";
    }

}

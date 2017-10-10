/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.nextsw.entities;

import java.io.Serializable;
import java.util.List;
import javax.persistence.Basic;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

/**
 *
 * @author Giovanni
 */
@Entity
@Table(name = "afferenza_struttura", catalog = "babel", schema = "organigramma")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "AfferenzaStruttura.findAll", query = "SELECT a FROM AfferenzaStruttura a")
    , @NamedQuery(name = "AfferenzaStruttura.findById", query = "SELECT a FROM AfferenzaStruttura a WHERE a.id = :id")
    , @NamedQuery(name = "AfferenzaStruttura.findByDescrizione", query = "SELECT a FROM AfferenzaStruttura a WHERE a.descrizione = :descrizione")})
public class AfferenzaStruttura implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @Column(name = "id")
    private Integer id;
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 200)
    @Column(name = "descrizione")
    private String descrizione;
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "idAfferenzaStruttura")
    private List<UtenteStruttura> utenteStrutturaList;

    public AfferenzaStruttura() {
    }

    public AfferenzaStruttura(Integer id) {
        this.id = id;
    }

    public AfferenzaStruttura(Integer id, String descrizione) {
        this.id = id;
        this.descrizione = descrizione;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getDescrizione() {
        return descrizione;
    }

    public void setDescrizione(String descrizione) {
        this.descrizione = descrizione;
    }

    @XmlTransient
    public List<UtenteStruttura> getUtenteStrutturaList() {
        return utenteStrutturaList;
    }

    public void setUtenteStrutturaList(List<UtenteStruttura> utenteStrutturaList) {
        this.utenteStrutturaList = utenteStrutturaList;
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
        if (!(object instanceof AfferenzaStruttura)) {
            return false;
        }
        AfferenzaStruttura other = (AfferenzaStruttura) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "it.nextsw.entities.AfferenzaStruttura[ id=" + id + " ]";
    }
    
}

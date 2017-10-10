/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.nextsw.entities;

import it.nextsw.olingo.interceptor.queryextension.BaseOdataJpaQueryExtension;
import java.io.Serializable;
import java.util.List;
import javax.persistence.Basic;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
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
//@EntityListeners(BaseOdataJpaQueryExtension.class)
@Table(name = "azienda", catalog = "babel", schema = "organigramma")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "Azienda.findAll", query = "SELECT a FROM Azienda a")
    , @NamedQuery(name = "Azienda.findById", query = "SELECT a FROM Azienda a WHERE a.id = :id")
    , @NamedQuery(name = "Azienda.findByCodice", query = "SELECT a FROM Azienda a WHERE a.codice = :codice")
    , @NamedQuery(name = "Azienda.findByNome", query = "SELECT a FROM Azienda a WHERE a.nome = :nome")
    , @NamedQuery(name = "Azienda.findByDescrizione", query = "SELECT a FROM Azienda a WHERE a.descrizione = :descrizione")
    , @NamedQuery(name = "Azienda.findByAoo", query = "SELECT a FROM Azienda a WHERE a.aoo = :aoo")})
public class Azienda implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @Column(name = "id")
    private Integer id;
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 30)
    @Column(name = "codice")
    private String codice;
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 250)
    @Column(name = "nome")
    private String nome;
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 1000)
    @Column(name = "descrizione")
    private String descrizione;
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 20)
    @Column(name = "aoo")
    private String aoo;
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "idAzienda")
    private List<Struttura> strutturaList;

    public Azienda() {
    }

    public Azienda(Integer id) {
        this.id = id;
    }

    public Azienda(Integer id, String codice, String nome, String descrizione, String aoo) {
        this.id = id;
        this.codice = codice;
        this.nome = nome;
        this.descrizione = descrizione;
        this.aoo = aoo;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getCodice() {
        return codice;
    }

    public void setCodice(String codice) {
        this.codice = codice;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getDescrizione() {
        return descrizione;
    }

    public void setDescrizione(String descrizione) {
        this.descrizione = descrizione;
    }

    public String getAoo() {
        return aoo;
    }

    public void setAoo(String aoo) {
        this.aoo = aoo;
    }

    @XmlTransient
    public List<Struttura> getStrutturaList() {
        return strutturaList;
    }

    public void setStrutturaList(List<Struttura> strutturaList) {
        this.strutturaList = strutturaList;
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
        if (!(object instanceof Azienda)) {
            return false;
        }
        Azienda other = (Azienda) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "it.nextsw.entities.Azienda[ id=" + id + " ]";
    }
    
}

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.bologna.ausl.baborg.entities;

import java.io.Serializable;
import java.util.List;
import javax.persistence.Basic;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 *
 * @author gdm
 */
@Entity
@Table(name = "pec", catalog = "babel", schema = "organigramma")
@NamedQueries({
    @NamedQuery(name = "Pec.findAll", query = "SELECT p FROM Pec p")})
public class Pec implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @Column(name = "id")
    private Integer id;
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 2147483647)
    @Column(name = "indirizzo")
    private String indirizzo;
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 2147483647)
    @Column(name = "username")
    private String username;
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 2147483647)
    @Column(name = "password")
    private String password;
    @Basic(optional = false)
    @NotNull
    @Column(name = "attiva")
    private boolean attiva;
    @Basic(optional = false)
    @NotNull
    @Column(name = "message_policy")
    private int messagePolicy;
    @Basic(optional = false)
    @NotNull
    @Column(name = "per_riservato")
    private boolean perRiservato;
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "idPec", fetch = FetchType.LAZY)
    private List<PecStruttura> pecStrutturaList;
    @JoinColumn(name = "id_pec_provider", referencedColumnName = "id")
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private PecProvider idPecProvider;

    public Pec() {
    }

    public Pec(Integer id) {
        this.id = id;
    }

    public Pec(Integer id, String indirizzo, String username, String password, boolean attiva, int messagePolicy, boolean perRiservato) {
        this.id = id;
        this.indirizzo = indirizzo;
        this.username = username;
        this.password = password;
        this.attiva = attiva;
        this.messagePolicy = messagePolicy;
        this.perRiservato = perRiservato;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getIndirizzo() {
        return indirizzo;
    }

    public void setIndirizzo(String indirizzo) {
        this.indirizzo = indirizzo;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public boolean getAttiva() {
        return attiva;
    }

    public void setAttiva(boolean attiva) {
        this.attiva = attiva;
    }

    public int getMessagePolicy() {
        return messagePolicy;
    }

    public void setMessagePolicy(int messagePolicy) {
        this.messagePolicy = messagePolicy;
    }

    public boolean getPerRiservato() {
        return perRiservato;
    }

    public void setPerRiservato(boolean perRiservato) {
        this.perRiservato = perRiservato;
    }

    public List<PecStruttura> getPecStrutturaList() {
        return pecStrutturaList;
    }

    public void setPecStrutturaList(List<PecStruttura> pecStrutturaList) {
        this.pecStrutturaList = pecStrutturaList;
    }

    public PecProvider getIdPecProvider() {
        return idPecProvider;
    }

    public void setIdPecProvider(PecProvider idPecProvider) {
        this.idPecProvider = idPecProvider;
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
        if (!(object instanceof Pec)) {
            return false;
        }
        Pec other = (Pec) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "it.bologna.ausl.baborg.entities.Pec[ id=" + id + " ]";
    }
    
}

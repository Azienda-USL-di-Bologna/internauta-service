/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.bologna.ausl.baborg.entities;

import java.io.Serializable;
import javax.persistence.Basic;
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
import javax.persistence.Table;
import javax.validation.constraints.NotNull;

/**
 *
 * @author gdm
 */
@Entity
@Table(name = "pec_struttura", catalog = "babel", schema = "organigramma")
@NamedQueries({
    @NamedQuery(name = "PecStruttura.findAll", query = "SELECT p FROM PecStruttura p")})
public class PecStruttura implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @Column(name = "id")
    private Integer id;
    @Basic(optional = false)
    @NotNull
    @Column(name = "principale")
    private boolean principale;
    @Basic(optional = false)
    @NotNull
    @Column(name = "propaga_strutture_figlie")
    private boolean propagaStruttureFiglie;
    @JoinColumn(name = "id_pec", referencedColumnName = "id")
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private Pec idPec;
    @JoinColumn(name = "id_struttura", referencedColumnName = "id")
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private Struttura idStruttura;

    public PecStruttura() {
    }

    public PecStruttura(Integer id) {
        this.id = id;
    }

    public PecStruttura(Integer id, boolean principale, boolean propagaStruttureFiglie) {
        this.id = id;
        this.principale = principale;
        this.propagaStruttureFiglie = propagaStruttureFiglie;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public boolean getPrincipale() {
        return principale;
    }

    public void setPrincipale(boolean principale) {
        this.principale = principale;
    }

    public boolean getPropagaStruttureFiglie() {
        return propagaStruttureFiglie;
    }

    public void setPropagaStruttureFiglie(boolean propagaStruttureFiglie) {
        this.propagaStruttureFiglie = propagaStruttureFiglie;
    }

    public Pec getIdPec() {
        return idPec;
    }

    public void setIdPec(Pec idPec) {
        this.idPec = idPec;
    }

    public Struttura getIdStruttura() {
        return idStruttura;
    }

    public void setIdStruttura(Struttura idStruttura) {
        this.idStruttura = idStruttura;
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
        if (!(object instanceof PecStruttura)) {
            return false;
        }
        PecStruttura other = (PecStruttura) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "it.bologna.ausl.baborg.entities.PecStruttura[ id=" + id + " ]";
    }
    
}

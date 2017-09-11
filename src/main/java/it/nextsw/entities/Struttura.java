/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.nextsw.entities;

import java.io.Serializable;
import java.util.Date;
import java.util.List;
import javax.persistence.Basic;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

/**
 *
 * @author Giovanni
 */
@Entity
@Table(name = "struttura", catalog = "babel", schema = "organigramma")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "Struttura.findAll", query = "SELECT s FROM Struttura s")
    , @NamedQuery(name = "Struttura.findById", query = "SELECT s FROM Struttura s WHERE s.id = :id")
    , @NamedQuery(name = "Struttura.findByCodice", query = "SELECT s FROM Struttura s WHERE s.codice = :codice")
    , @NamedQuery(name = "Struttura.findByCodiceStrutturaPadre", query = "SELECT s FROM Struttura s WHERE s.codiceStrutturaPadre = :codiceStrutturaPadre")
    , @NamedQuery(name = "Struttura.findByNome", query = "SELECT s FROM Struttura s WHERE s.nome = :nome")
    , @NamedQuery(name = "Struttura.findByCodiceDislocazione", query = "SELECT s FROM Struttura s WHERE s.codiceDislocazione = :codiceDislocazione")
    , @NamedQuery(name = "Struttura.findByDislocazione", query = "SELECT s FROM Struttura s WHERE s.dislocazione = :dislocazione")
    , @NamedQuery(name = "Struttura.findByDataAttivazione", query = "SELECT s FROM Struttura s WHERE s.dataAttivazione = :dataAttivazione")
    , @NamedQuery(name = "Struttura.findByDataCessazione", query = "SELECT s FROM Struttura s WHERE s.dataCessazione = :dataCessazione")
    , @NamedQuery(name = "Struttura.findByAttiva", query = "SELECT s FROM Struttura s WHERE s.attiva = :attiva")
    , @NamedQuery(name = "Struttura.findBySpettrale", query = "SELECT s FROM Struttura s WHERE s.spettrale = :spettrale")})
public class Struttura implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @Column(name = "id")
    private Integer id;
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 100)
    @Column(name = "codice")
    private String codice;
    @Size(max = 100)
    @Column(name = "codice_struttura_padre")
    private String codiceStrutturaPadre;
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 400)
    @Column(name = "nome")
    private String nome;
    @Size(max = 10)
    @Column(name = "codice_dislocazione")
    private String codiceDislocazione;
    @Size(max = 100)
    @Column(name = "dislocazione")
    private String dislocazione;
    @Column(name = "data_attivazione")
    @Temporal(TemporalType.TIMESTAMP)
    private Date dataAttivazione;
    @Column(name = "data_cessazione")
    @Temporal(TemporalType.TIMESTAMP)
    private Date dataCessazione;
    @Basic(optional = false)
    @NotNull
    @Column(name = "attiva")
    private boolean attiva;
    @Basic(optional = false)
    @NotNull
    @Column(name = "spettrale")
    private boolean spettrale;
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "idStruttura")
    private List<UtenteStruttura> utenteStrutturaList;
    @JoinColumn(name = "id_azienda", referencedColumnName = "id")
    @ManyToOne(optional = false)
    private Azienda idAzienda;
    @OneToMany(mappedBy = "idStrutturaPadre")
    private List<Struttura> strutturaList;
    @JoinColumn(name = "id_struttura_padre", referencedColumnName = "id")
    @ManyToOne
    private Struttura idStrutturaPadre;

    public Struttura() {
    }

    public Struttura(Integer id) {
        this.id = id;
    }

    public Struttura(Integer id, String codice, String nome, boolean attiva, boolean spettrale) {
        this.id = id;
        this.codice = codice;
        this.nome = nome;
        this.attiva = attiva;
        this.spettrale = spettrale;
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

    public String getCodiceStrutturaPadre() {
        return codiceStrutturaPadre;
    }

    public void setCodiceStrutturaPadre(String codiceStrutturaPadre) {
        this.codiceStrutturaPadre = codiceStrutturaPadre;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getCodiceDislocazione() {
        return codiceDislocazione;
    }

    public void setCodiceDislocazione(String codiceDislocazione) {
        this.codiceDislocazione = codiceDislocazione;
    }

    public String getDislocazione() {
        return dislocazione;
    }

    public void setDislocazione(String dislocazione) {
        this.dislocazione = dislocazione;
    }

    public Date getDataAttivazione() {
        return dataAttivazione;
    }

    public void setDataAttivazione(Date dataAttivazione) {
        this.dataAttivazione = dataAttivazione;
    }

    public Date getDataCessazione() {
        return dataCessazione;
    }

    public void setDataCessazione(Date dataCessazione) {
        this.dataCessazione = dataCessazione;
    }

    public boolean getAttiva() {
        return attiva;
    }

    public void setAttiva(boolean attiva) {
        this.attiva = attiva;
    }

    public boolean getSpettrale() {
        return spettrale;
    }

    public void setSpettrale(boolean spettrale) {
        this.spettrale = spettrale;
    }

    @XmlTransient
    public List<UtenteStruttura> getUtenteStrutturaList() {
        return utenteStrutturaList;
    }

    public void setUtenteStrutturaList(List<UtenteStruttura> utenteStrutturaList) {
        this.utenteStrutturaList = utenteStrutturaList;
    }

    public Azienda getIdAzienda() {
        return idAzienda;
    }

    public void setIdAzienda(Azienda idAzienda) {
        this.idAzienda = idAzienda;
    }

    @XmlTransient
    public List<Struttura> getStrutturaList() {
        return strutturaList;
    }

    public void setStrutturaList(List<Struttura> strutturaList) {
        this.strutturaList = strutturaList;
    }

    public Struttura getIdStrutturaPadre() {
        return idStrutturaPadre;
    }

    public void setIdStrutturaPadre(Struttura idStrutturaPadre) {
        this.idStrutturaPadre = idStrutturaPadre;
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
        if (!(object instanceof Struttura)) {
            return false;
        }
        Struttura other = (Struttura) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "it.nextsw.entities.Struttura[ id=" + id + " ]";
    }
    
}

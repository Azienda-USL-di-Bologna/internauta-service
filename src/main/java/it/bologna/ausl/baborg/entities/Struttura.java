package it.bologna.ausl.baborg.entities;

import java.io.Serializable;
import java.util.Date;
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
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 *
 * @author gdm
 */
@Entity
@Table(name = "struttura", catalog = "babel", schema = "organigramma")
@NamedQueries({
    @NamedQuery(name = "Struttura.findAll", query = "SELECT s FROM Struttura s")})
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
    @Basic(optional = false)
    @NotNull
    @Column(name = "usa_segreteria_bucata_padre")
    private boolean usaSegreteriaBucataPadre;
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "idStruttura", fetch = FetchType.LAZY)
    private List<UtenteStruttura> utenteStrutturaList;
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "idStruttura", fetch = FetchType.LAZY)
    private List<PecStruttura> pecStrutturaList;
    @JoinColumn(name = "id_azienda", referencedColumnName = "id")
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private Azienda idAzienda;
    
    @OneToMany(mappedBy = "idStrutturaPadre", fetch = FetchType.LAZY)
    private List<Struttura> strutturaPadreList;
    
    @JoinColumn(name = "id_struttura_padre", referencedColumnName = "id")
    @ManyToOne(fetch = FetchType.LAZY)
    private Struttura idStrutturaPadre;
    
    @OneToMany(mappedBy = "idStrutturaSegreteria", fetch = FetchType.LAZY)
    private List<Struttura> strutturaSegreteriaList;
    
    @JoinColumn(name = "id_struttura_segreteria", referencedColumnName = "id")
    @ManyToOne(fetch = FetchType.LAZY)
    private Struttura idStrutturaSegreteria;

    public Struttura() {
    }

    public Struttura(Integer id) {
        this.id = id;
    }

    public Struttura(Integer id, String codice, String nome, boolean attiva, boolean spettrale, boolean usaSegreteriaBucataPadre) {
        this.id = id;
        this.codice = codice;
        this.nome = nome;
        this.attiva = attiva;
        this.spettrale = spettrale;
        this.usaSegreteriaBucataPadre = usaSegreteriaBucataPadre;
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

    public boolean getUsaSegreteriaBucataPadre() {
        return usaSegreteriaBucataPadre;
    }

    public void setUsaSegreteriaBucataPadre(boolean usaSegreteriaBucataPadre) {
        this.usaSegreteriaBucataPadre = usaSegreteriaBucataPadre;
    }

    public List<UtenteStruttura> getUtenteStrutturaList() {
        return utenteStrutturaList;
    }

    public void setUtenteStrutturaList(List<UtenteStruttura> utenteStrutturaList) {
        this.utenteStrutturaList = utenteStrutturaList;
    }

    public List<PecStruttura> getPecStrutturaList() {
        return pecStrutturaList;
    }

    public void setPecStrutturaList(List<PecStruttura> pecStrutturaList) {
        this.pecStrutturaList = pecStrutturaList;
    }

    public Azienda getIdAzienda() {
        return idAzienda;
    }

    public void setIdAzienda(Azienda idAzienda) {
        this.idAzienda = idAzienda;
    }

    public List<Struttura> getStrutturaPadreList() {
        return strutturaPadreList;
    }

    public void setStrutturaPadreList(List<Struttura> strutturaPadreList) {
        this.strutturaPadreList = strutturaPadreList;
    }

    public Struttura getIdStrutturaPadre() {
        return idStrutturaPadre;
    }

    public void setIdStrutturaPadre(Struttura idStrutturaPadre) {
        this.idStrutturaPadre = idStrutturaPadre;
    }

    public List<Struttura> getStrutturaSegreteriaList() {
        return strutturaSegreteriaList;
    }

    public void setStrutturaSegreteriaList(List<Struttura> strutturaSegreteriaList) {
        this.strutturaSegreteriaList = strutturaSegreteriaList;
    }

    public Struttura getIdStrutturaSegreteria() {
        return idStrutturaSegreteria;
    }

    public void setIdStrutturaSegreteria(Struttura idStrutturaSegreteria) {
        this.idStrutturaSegreteria = idStrutturaSegreteria;
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
        return "it.bologna.ausl.baborg.entities.Struttura[ id=" + id + " ]";
    }
    
}

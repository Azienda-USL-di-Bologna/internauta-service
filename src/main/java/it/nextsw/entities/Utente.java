/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.nextsw.entities;

import it.nextsw.olingo.interceptor.queryextension.BaseOdataJpaQueryExtension;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import javax.persistence.Basic;
import javax.persistence.CascadeType;
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
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/**
 *
 * @author Giovanni
 */
@Entity
@EntityListeners(BaseOdataJpaQueryExtension.class)
@Table(name = "utente", catalog = "babel", schema = "organigramma")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "Utente.findAll", query = "SELECT u FROM Utente u")
    , @NamedQuery(name = "Utente.findById", query = "SELECT u FROM Utente u WHERE u.id = :id")
    , @NamedQuery(name = "Utente.findByUsername", query = "SELECT u FROM Utente u WHERE u.username = :username")
    , @NamedQuery(name = "Utente.findByMatricola", query = "SELECT u FROM Utente u WHERE u.matricola = :matricola")
    , @NamedQuery(name = "Utente.findByNome", query = "SELECT u FROM Utente u WHERE u.nome = :nome")
    , @NamedQuery(name = "Utente.findByCognome", query = "SELECT u FROM Utente u WHERE u.cognome = :cognome")
    , @NamedQuery(name = "Utente.findByCodiceFiscale", query = "SELECT u FROM Utente u WHERE u.codiceFiscale = :codiceFiscale")
    , @NamedQuery(name = "Utente.findByDescrizione", query = "SELECT u FROM Utente u WHERE u.descrizione = :descrizione")
    , @NamedQuery(name = "Utente.findByIdRuolo", query = "SELECT u FROM Utente u WHERE u.idRuolo = :idRuolo")
    , @NamedQuery(name = "Utente.findByEmail", query = "SELECT u FROM Utente u WHERE u.email = :email")
    , @NamedQuery(name = "Utente.findByIdInquadramento", query = "SELECT u FROM Utente u WHERE u.idInquadramento = :idInquadramento")
    , @NamedQuery(name = "Utente.findByTelefono", query = "SELECT u FROM Utente u WHERE u.telefono = :telefono")
    , @NamedQuery(name = "Utente.findByFax", query = "SELECT u FROM Utente u WHERE u.fax = :fax")
    , @NamedQuery(name = "Utente.findByOmonimia", query = "SELECT u FROM Utente u WHERE u.omonimia = :omonimia")
    , @NamedQuery(name = "Utente.findByPasswordHash", query = "SELECT u FROM Utente u WHERE u.passwordHash = :passwordHash")
    , @NamedQuery(name = "Utente.findByDominio", query = "SELECT u FROM Utente u WHERE u.dominio = :dominio")
    , @NamedQuery(name = "Utente.findByAttivo", query = "SELECT u FROM Utente u WHERE u.attivo = :attivo")
    , @NamedQuery(name = "Utente.findByCodiceStruttura", query = "SELECT u FROM Utente u WHERE u.codiceStruttura = :codiceStruttura")})
public class Utente implements Serializable, UserDetails {

    @JoinColumn(name = "id_ruolo", referencedColumnName = "id")
    @ManyToOne(optional = false)
    private Ruolo idRuolo;

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @Column(name = "id")
    private Integer id;
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 250)
    @Column(name = "username")
    private String username;
    @Size(max = 100)
    @Column(name = "matricola")
    private String matricola;
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 100)
    @Column(name = "nome")
    private String nome;
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 100)
    @Column(name = "cognome")
    private String cognome;
    @Size(max = 16)
    @Column(name = "codice_fiscale")
    private String codiceFiscale;
    @Size(max = 200)
    @Column(name = "descrizione")
    private String descrizione;
    // @Pattern(regexp="[a-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\\.[a-z0-9!#$%&'*+/=?^_`{|}~-]+)*@(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\\.)+[a-z0-9](?:[a-z0-9-]*[a-z0-9])?", message="Invalid email")//if the field contains email address consider using this annotation to enforce field validation
    @Size(max = 400)
    @Column(name = "email")
    private String email;
    @Size(max = 5)
    @Column(name = "id_inquadramento")
    private String idInquadramento;
    @Size(max = 150)
    @Column(name = "telefono")
    private String telefono;
    // @Pattern(regexp="^\\(?(\\d{3})\\)?[- ]?(\\d{3})[- ]?(\\d{4})$", message="Invalid phone/fax format, should be as xxx-xxx-xxxx")//if the field contains phone or fax number consider using this annotation to enforce field validation
    @Size(max = 150)
    @Column(name = "fax")
    private String fax;
    @Basic(optional = false)
    @NotNull
    @Column(name = "omonimia")
    private boolean omonimia;
    @Size(max = 200)
    @Column(name = "password_hash")
    private String passwordHash;
    @Size(max = 50)
    @Column(name = "dominio")
    private String dominio;
    @Basic(optional = false)
    @NotNull
    @Column(name = "attivo")
    private boolean attivo;
    @Size(max = 100)
    @Column(name = "codice_struttura")
    private String codiceStruttura;
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "idUtente")
    private List<UtenteStruttura> utenteStrutturaList;

    public Utente() {
    }

    public Utente(Integer id) {
        this.id = id;
    }

    public Utente(Integer id, String username, String nome, String cognome, boolean omonimia, boolean attivo) {
        this.id = id;
        this.username = username;
        this.nome = nome;
        this.cognome = cognome;
        this.omonimia = omonimia;
        this.attivo = attivo;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getMatricola() {
        return matricola;
    }

    public void setMatricola(String matricola) {
        this.matricola = matricola;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getCognome() {
        return cognome;
    }

    public void setCognome(String cognome) {
        this.cognome = cognome;
    }

    public String getCodiceFiscale() {
        return codiceFiscale;
    }

    public void setCodiceFiscale(String codiceFiscale) {
        this.codiceFiscale = codiceFiscale;
    }

    public String getDescrizione() {
        return descrizione;
    }

    public void setDescrizione(String descrizione) {
        this.descrizione = descrizione;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getIdInquadramento() {
        return idInquadramento;
    }

    public void setIdInquadramento(String idInquadramento) {
        this.idInquadramento = idInquadramento;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public String getFax() {
        return fax;
    }

    public void setFax(String fax) {
        this.fax = fax;
    }

    public boolean getOmonimia() {
        return omonimia;
    }

    public void setOmonimia(boolean omonimia) {
        this.omonimia = omonimia;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getDominio() {
        return dominio;
    }

    public void setDominio(String dominio) {
        this.dominio = dominio;
    }

    public boolean getAttivo() {
        return attivo;
    }

    public void setAttivo(boolean attivo) {
        this.attivo = attivo;
    }

    public String getCodiceStruttura() {
        return codiceStruttura;
    }

    public void setCodiceStruttura(String codiceStruttura) {
        this.codiceStruttura = codiceStruttura;
    }

    @XmlTransient
    public List<UtenteStruttura> getUtenteStrutturaList() {
        return utenteStrutturaList;
    }

    public void setUtenteStrutturaList(List<UtenteStruttura> utenteStrutturaList) {
        this.utenteStrutturaList = utenteStrutturaList;
    }

    // Override per userdetails
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Arrays.asList(new SimpleGrantedAuthority("ADMIN"));
    }

    @Override
    public String getPassword() {
        return getPasswordHash();
    }


    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return getAttivo();
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
        if (!(object instanceof Utente)) {
            return false;
        }
        Utente other = (Utente) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "it.nextsw.entities.Utente[ id=" + id + " ]";
    }

    public Ruolo getIdRuolo() {
        return idRuolo;
    }

    public void setIdRuolo(Ruolo idRuolo) {
        this.idRuolo = idRuolo;
    }
    
}

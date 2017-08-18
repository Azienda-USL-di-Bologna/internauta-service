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
@Table(name = "pec_provider", catalog = "babel", schema = "organigramma")
@NamedQueries({
    @NamedQuery(name = "PecProvider.findAll", query = "SELECT p FROM PecProvider p")})
public class PecProvider implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @Column(name = "id")
    private Integer id;
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 2147483647)
    @Column(name = "descrizione")
    private String descrizione;
    @Basic(optional = false)
    @NotNull
    @Column(name = "pec")
    private boolean pec;
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 2147483647)
    @Column(name = "host")
    private String host;
    @Basic(optional = false)
    @NotNull
    @Column(name = "port")
    private int port;
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 2147483647)
    @Column(name = "protocol")
    private String protocol;
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 2147483647)
    @Column(name = "host_out")
    private String hostOut;
    @Basic(optional = false)
    @NotNull
    @Column(name = "port_out")
    private int portOut;
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 2147483647)
    @Column(name = "protocol_out")
    private String protocolOut;
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "idPecProvider", fetch = FetchType.LAZY)
    private List<Pec> pecList;

    public PecProvider() {
    }

    public PecProvider(Integer id) {
        this.id = id;
    }

    public PecProvider(Integer id, String descrizione, boolean pec, String host, int port, String protocol, String hostOut, int portOut, String protocolOut) {
        this.id = id;
        this.descrizione = descrizione;
        this.pec = pec;
        this.host = host;
        this.port = port;
        this.protocol = protocol;
        this.hostOut = hostOut;
        this.portOut = portOut;
        this.protocolOut = protocolOut;
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

    public boolean getPec() {
        return pec;
    }

    public void setPec(boolean pec) {
        this.pec = pec;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public String getHostOut() {
        return hostOut;
    }

    public void setHostOut(String hostOut) {
        this.hostOut = hostOut;
    }

    public int getPortOut() {
        return portOut;
    }

    public void setPortOut(int portOut) {
        this.portOut = portOut;
    }

    public String getProtocolOut() {
        return protocolOut;
    }

    public void setProtocolOut(String protocolOut) {
        this.protocolOut = protocolOut;
    }

    public List<Pec> getPecList() {
        return pecList;
    }

    public void setPecList(List<Pec> pecList) {
        this.pecList = pecList;
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
        if (!(object instanceof PecProvider)) {
            return false;
        }
        PecProvider other = (PecProvider) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "it.bologna.ausl.baborg.entities.PecProvider[ id=" + id + " ]";
    }
    
}

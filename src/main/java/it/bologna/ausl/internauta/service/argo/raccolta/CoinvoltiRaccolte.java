/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.bologna.ausl.internauta.service.argo.raccolta;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import it.nextsw.common.annotations.NextSdrAncestor;
import java.io.Serializable;
import java.time.ZonedDateTime;
import javax.persistence.Basic;
import javax.persistence.Cacheable;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Version;
import org.springframework.format.annotation.DateTimeFormat;

/**
 *
 * @author Matteo Next
 */
//@Entity
//@Table(name = "coinvolti_raccolte", catalog = "argo908", schema = "gd")
//@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
//@Cacheable(false)
//@JsonIdentityInfo(generator = ObjectIdGenerators.IntSequenceGenerator.class, property = "@id", scope = Raccolta.class)
public class CoinvoltiRaccolte implements Serializable {

//    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    @Basic(optional = false)
//    @Column(name = "id")
    private Long id;
    
//    @NextSdrAncestor(relationName = "idCoinvoltoRaccoltaSemplice")
//    @JoinColumn(name = "id_coinvolto", referencedColumnName = "id")
//    @ManyToOne(fetch = FetchType.EAGER, cascade = {CascadeType.MERGE, CascadeType.PERSIST})
    private Long idCoinvolto;
    
//    @NextSdrAncestor(relationName = "idRaccoltaRaccoltaSemplice")
//    @JoinColumn(name = "id_raccolta", referencedColumnName = "id")
//    @ManyToOne(fetch = FetchType.EAGER, cascade = {CascadeType.MERGE, CascadeType.PERSIST})
    private Long idRaccolta;
    
//    @Version()
//    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX'['VV']'")
//    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX'['VV']'")
    private ZonedDateTime version;

    public Long getIdCoinvolto() {
        return idCoinvolto;
    }

    public void setIdCoinvolto(Long idCoinvolto) {
        this.idCoinvolto = idCoinvolto;
    }

    public Long getIdRaccolta() {
        return idRaccolta;
    }

    public void setIdRaccolta(Long idRaccolta) {
        this.idRaccolta = idRaccolta;
    }

    public ZonedDateTime getVersion() {
        return version;
    }

    public void setVersion(ZonedDateTime version) {
        this.version = version;
    }
    
    
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
    
    
    
}

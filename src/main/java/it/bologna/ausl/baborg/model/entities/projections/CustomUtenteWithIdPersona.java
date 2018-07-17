/**
 * Auto-Generated using the Jenesis Syntax API
 */
package it.bologna.ausl.baborg.model.entities.projections;

import com.fasterxml.jackson.annotation.JsonFormat;
import it.bologna.ausl.baborg.model.entities.Persona;

import it.bologna.ausl.baborg.model.entities.Utente;

import org.springframework.beans.factory.annotation.Value;

import org.springframework.data.rest.core.config.Projection;


@Projection(name = "CustomUtenteWithIdPersona", types = it.bologna.ausl.baborg.model.entities.Utente.class)
public interface CustomUtenteWithIdPersona {
    public Boolean getAttivo();

    public Integer getBitRuoli();

    public String getCodiceFiscale();

    public String getCognome();

    public String getDescrizione();

    public Integer getDominio();

    public String getEmail();


    public String getFax();

    public Integer getId();

    public String getIdInquadramento();

    public Integer getIdRuoloAziendale();

    public String getMatricola();

    public String getNome();

    public Boolean getOmonimia();

    public String getPasswordHash();

    public String getTelefono();

    public String getUsername();
    
    public Persona getIdPersona();
    
    @Value("#{@foreignKeyExporter.toForeignKey('idAzienda', target)}")
    public it.bologna.ausl.jenesisprojections.tools.ForeignKey getFK_idAzienda();

    @Value("#{@foreignKeyExporter.toForeignKey('idPersona', target)}")
    public it.bologna.ausl.jenesisprojections.tools.ForeignKey getFK_idPersona();

    @Value("#{@foreignKeyExporter.toForeignKey('pecUtenteSet', target)}")
    public it.bologna.ausl.jenesisprojections.tools.ForeignKey getFK_pecUtenteSet();

    @Value("#{@foreignKeyExporter.toForeignKey('utenteStrutturaSet', target)}")
    public it.bologna.ausl.jenesisprojections.tools.ForeignKey getFK_utenteStrutturaSet();

}

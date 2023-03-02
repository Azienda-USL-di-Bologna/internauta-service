package it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.lanciatrasformatore;

import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.JobWorkerData;

/**
 *
 * @author Top
 */
public class LanciaTrasformatoreJobWorkerData extends JobWorkerData{
    private Integer idAzienda;
    private Boolean ribaltaArgo; 
    private Boolean ribaltaInternauta;
    private String email;
    private String fonteRibaltone;
    private Boolean trasforma;
    private Integer IdUtente;
    private String note;

    public LanciaTrasformatoreJobWorkerData() { }

    public LanciaTrasformatoreJobWorkerData(Integer idAzienda, Boolean ribaltaArgo, Boolean ribaltaInternauta, String email, String fonteRibaltone, Boolean trasforma, Integer IdUtente, String note) {
        this.idAzienda = idAzienda;
        this.ribaltaArgo = ribaltaArgo;
        this.ribaltaInternauta = ribaltaInternauta;
        this.email = email;
        this.fonteRibaltone = fonteRibaltone;
        this.trasforma = trasforma;
        this.IdUtente = IdUtente;
        this.note = note;
    }

    public Integer getIdAzienda() {
        return idAzienda;
    }

    public void setIdAzienda(Integer idAzienda) {
        this.idAzienda = idAzienda;
    }

    public Boolean getRibaltaArgo() {
        return ribaltaArgo;
    }

    public void setRibaltaArgo(Boolean ribaltaArgo) {
        this.ribaltaArgo = ribaltaArgo;
    }

    public Boolean getRibaltaInternauta() {
        return ribaltaInternauta;
    }

    public void setRibaltaInternauta(Boolean ribaltaInternauta) {
        this.ribaltaInternauta = ribaltaInternauta;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFonteRibaltone() {
        return fonteRibaltone;
    }

    public void setFonteRibaltone(String fonteRibaltone) {
        this.fonteRibaltone = fonteRibaltone;
    }

    public Boolean getTrasforma() {
        return trasforma;
    }

    public void setTrasforma(Boolean trasforma) {
        this.trasforma = trasforma;
    }

    public Integer getIdUtente() {
        return IdUtente;
    }

    public void setIdUtente(Integer IdUtente) {
        this.IdUtente = IdUtente;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
    
    
            
}

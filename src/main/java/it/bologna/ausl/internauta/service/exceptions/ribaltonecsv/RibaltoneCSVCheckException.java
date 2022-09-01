package it.bologna.ausl.internauta.service.exceptions.ribaltonecsv;

/**
 *
 * @author mdonza
 */
public class RibaltoneCSVCheckException extends Exception{
    private String operazione;
    private Object dato;
    
    public RibaltoneCSVCheckException(String message) {
        super(message);
    }

    public RibaltoneCSVCheckException(String message, Throwable cause) {
        super(message, cause);
    }

    public RibaltoneCSVCheckException(String operazione, Object dato, String message) {
        super(message);
        this.operazione = operazione;
        this.dato = dato;
    }
    
    public RibaltoneCSVCheckException(String operazione, Object dato, String message, Throwable cause) {
        super(message, cause);
        this.operazione = operazione;
        this.dato = dato;
    }
    
    public String getOperazione() {
        return operazione;
    }

    public void setOperazione(String operazione) {
        this.operazione = operazione;
    }

    public Object getDato() {
        return dato;
    }

    public void setDato(Object dato) {
        this.dato = dato;
    }
    
}

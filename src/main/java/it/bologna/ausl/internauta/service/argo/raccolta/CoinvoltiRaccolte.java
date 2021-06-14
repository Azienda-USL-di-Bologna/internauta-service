package it.bologna.ausl.internauta.service.argo.raccolta;

import java.io.Serializable;
import java.time.ZonedDateTime;

/**
 *
 * @author Matteo Next
 */
public class CoinvoltiRaccolte implements Serializable {

    private Long id;
    private Long idCoinvolto;
    private Long idRaccolta;
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

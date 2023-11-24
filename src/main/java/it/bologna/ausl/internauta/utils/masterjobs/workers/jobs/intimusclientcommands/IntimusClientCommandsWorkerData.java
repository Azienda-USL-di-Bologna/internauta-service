package it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.intimusclientcommands;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import it.bologna.ausl.internauta.service.utils.IntimusUtils;
import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.JobWorkerData;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author conte
 */
public class IntimusClientCommandsWorkerData extends JobWorkerData {
    @JsonIgnore
    private static final Logger log = LoggerFactory.getLogger(IntimusClientCommandsWorkerData.class);

    @JsonProperty("id_persona")
    private Integer idPersona;

    @JsonProperty("id_aziende")
    private Integer[] idAziende;

    @JsonProperty("apps")
    private String[] apps;

    @JsonProperty("all_aziende")
    private Boolean allAziende;
    
    @JsonProperty("params")
    @JsonInclude(value=JsonInclude.Include.NON_EMPTY, content=JsonInclude.Include.NON_NULL)
    private Map<String, Object> params;

    @JsonProperty("command")
    private IntimusUtils.IntimusCommandNames command;

    public IntimusClientCommandsWorkerData(Integer idPersona, Integer[] idAziende, String[] apps, Boolean allAziende, Map<String, Object> params, IntimusUtils.IntimusCommandNames command) {
        this.idPersona = idPersona;
        this.idAziende = idAziende;
        this.apps = apps;
        this.allAziende = allAziende;
        this.params = params;
        this.command = command;
    }

    public Integer getIdPersona() {
        return idPersona;
    }

    public void setIdPersona(Integer idPersona) {
        this.idPersona = idPersona;
    }

    public Integer[] getIdAziende() {
        return idAziende;
    }

    public void setIdAziende(Integer[] idAziende) {
        this.idAziende = idAziende;
    }

    public String[] getApps() {
        return apps;
    }

    public void setApps(String[] apps) {
        this.apps = apps;
    }

    public Boolean getAllAziende() {
        return allAziende;
    }

    public void setAllAziende(Boolean allAziende) {
        this.allAziende = allAziende;
    }

    public Map<String, Object> getParams() {
        return params;
    }

    public void setParams(Map<String, Object> params) {
        this.params = params;
    }

    public IntimusUtils.IntimusCommandNames getCommand() {
        return command;
    }

    public void setCommand(IntimusUtils.IntimusCommandNames command) {
        this.command = command;
    }

}


package it.bologna.ausl.internauta.service.configuration.utils;

/**
 *
 * @author guido
 */
public class AziendaParams {
    
    private String codiceAzienda;
    private String jdbcUrl;
    private String dbUsername;
    private String dbPassword;
    private String storageConnString;
    private String babelSuiteWebApiUrl;

    public AziendaParams() {
    }

    public String getCodiceAzienda() {
        return codiceAzienda;
    }

    public void setCodiceAzienda(String codiceAzienda) {
        this.codiceAzienda = codiceAzienda;
    }
    
    public String getJdbcUrl() {
        return jdbcUrl;
    }

    public void setJdbcUrl(String jdbcUrl) {
        this.jdbcUrl = jdbcUrl;
    }

        public String getDbUsername() {
        return dbUsername;
    }

    public void setDbUsername(String dbUsername) {
        this.dbUsername = dbUsername;
    }

    public String getDbPassword() {
        return dbPassword;
    }

    public void setDbPassword(String dbPassword) {
        this.dbPassword = dbPassword;
    }

    public String getStorageConnString() {
        return storageConnString;
    }

    public void setStorageConnString(String storageConnString) {
        this.storageConnString = storageConnString;
    }

    public String getBabelSuiteWebApiUrl() {
        return babelSuiteWebApiUrl;
    }

    public void setBabelSuiteWebApiUrl(String babelSuiteWebApiUrl) {
        this.babelSuiteWebApiUrl = babelSuiteWebApiUrl;
    }   
    
}

package it.bologna.ausl.internauta.service.controllers.rubrica.inad;

/**
 *
 * @author MicheleD'Onza
 * {
                "digitalAddress": "example@pec.it",
                "practicedProfession": "Avvocato",
                "usageInfo": {
                    "motivation": "CESSAZIONE_VOLONTARIA",
                    "dateEndValidity": "2017-07-21T17:32:28Z"
                }
            }
 */
public class DigitalAddress {
    private String digitalAddress;
    private String practicedProfession;
    private UsageInfo usageInfo;

    public String getDigitalAddress() {
        return digitalAddress;
    }

    public void setDigitalAddress(String digitalAddress) {
        this.digitalAddress = digitalAddress;
    }

    public String getPracticedProfession() {
        return practicedProfession;
    }

    public void setPracticedProfession(String practicedProfession) {
        this.practicedProfession = practicedProfession;
    }

    public UsageInfo getUsageInfo() {
        return usageInfo;
    }

    public void setUsageInfo(UsageInfo usageInfo) {
        this.usageInfo = usageInfo;
    }
    
}

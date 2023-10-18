/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package it.bologna.ausl.internauta.service.controllers.rubrica.inad;

import java.time.ZonedDateTime;

/**
 *
 * @author MicheleD'Onza
 */
class UsageInfo {
    private String motivation;
    private ZonedDateTime dateEndValidity;

    public String getMotivation() {
        return motivation;
    }

    public void setMotivation(String motivation) {
        this.motivation = motivation;
    }

    public ZonedDateTime getDateEndValidity() {
        return dateEndValidity;
    }

    public void setDateEndValidity(ZonedDateTime dateEndValidity) {
        this.dateEndValidity = dateEndValidity;
    }
    
    
}

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.nextsw.odata.complexTypes;

import it.nextsw.olingo.edmextension.annotation.EdmSimpleProperty;
import org.apache.olingo.odata2.api.annotation.edm.EdmComplexType;
import org.apache.olingo.odata2.api.edm.EdmSimpleTypeKind;
import org.springframework.stereotype.Component;



@EdmComplexType
@Component
public class OrderValue {

    @EdmSimpleProperty(type = EdmSimpleTypeKind.Double)
    private Double amount;
    @EdmSimpleProperty
    private String currency;

    public Double getAmount() {
        return amount;
    }
    
    public String getCurrency() {
        return currency;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }


    public void setCurrency(String currency) {
        this.currency = currency;
    }

}

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.bologna.ausl.entities.baborg.functionimports;

import java.util.List;
import org.apache.olingo.odata2.api.uri.info.GetFunctionImportUriInfo;
import org.apache.olingo.odata2.jpa.processor.api.ODataJPAContext;
import org.apache.olingo.odata2.jpa.processor.api.exception.ODataJPAModelException;
import org.apache.olingo.odata2.jpa.processor.api.exception.ODataJPARuntimeException;
import org.apache.olingo.odata2.jpa.processor.core.access.data.JPAProcessorImpl;

/**
 *
 * @author Utente
 */
public class test extends JPAProcessorImpl{
    
    public test(ODataJPAContext oDataJPAContext) {
        super(oDataJPAContext);
    }
    
    @SuppressWarnings("unchecked")
  @Override
  public List<Object> process(final GetFunctionImportUriInfo uriParserResultView)
      throws ODataJPAModelException, ODataJPARuntimeException {
      
      
      
      return process(uriParserResultView);
  }
    
}

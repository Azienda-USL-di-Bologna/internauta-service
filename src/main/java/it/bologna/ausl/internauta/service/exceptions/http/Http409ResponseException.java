package it.bologna.ausl.internauta.service.exceptions.http;

import org.springframework.http.HttpStatus;

/**
 *
 * @author spritz
 */
public class Http409ResponseException extends HttpInternautaResponseException {

  public Http409ResponseException(String code, String message, Throwable cause) {
    super(HttpStatus.CONFLICT, code, message, cause);
  }

  public Http409ResponseException(String code, String message) {
    super(HttpStatus.CONFLICT, code, message);
  }

  public Http409ResponseException(String code, Throwable cause) {
    super(HttpStatus.CONFLICT, code, cause);
  }

//  public Http409ResponseException() {
//    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
//  }
}

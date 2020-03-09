package eu.europeana.keycloak.exception;

/**
 * Error thrown when a request handled by the ForwardController cannot be forwarded
 * @author Patrick Ehlert
 * Created on Mar 2, 2020
 */
public class ForwardException extends RuntimeException {

    public ForwardException(String message, Throwable t) {
        super(message, t);
    }
}

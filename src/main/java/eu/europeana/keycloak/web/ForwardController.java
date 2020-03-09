package eu.europeana.keycloak.web;

import eu.europeana.keycloak.exception.ForwardException;
import org.apache.logging.log4j.LogManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.PostConstruct;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Provides short urls for user login and token requests to Europeana realm.
 * The exact url to which requests are fowarded is specified in the keycloak.properties file
 * @author Patrick Ehlert
 * Created on Feb 26, 2020
 */
@RestController
public class ForwardController {

    public static final String BASE_PATH_OIDC = "/oidc";

    private static final String BASE_PATH_LOGIN = "/login";

    @Value("${keycloak.forward.account-service:}")
    private String forwardLoginPath;

    @Value("${keycloak.forward.token-service}")
    private String forwardTokenPath;

    @PostConstruct
    private void validateConfig() {
        if (StringUtils.isEmpty(forwardLoginPath)) {
            LogManager.getLogger(ForwardController.class).error("Account service forward path not configured!");
        }
        if (StringUtils.isEmpty(forwardTokenPath)) {
            LogManager.getLogger(ForwardController.class).error("Token service forward path not configured!");
        }
        if (!forwardTokenPath.endsWith("/")) {
            forwardTokenPath += '/';
        }
    }

    @GetMapping(BASE_PATH_LOGIN)
    public void fowardLoginGet(HttpServletRequest request, HttpServletResponse response) {
        forward(forwardLoginPath, request, response);
    }

    /**
     * Forwards certain OIDC GET requests to the appropriate Keycloak endpoints
     */
    @GetMapping({BASE_PATH_OIDC + "/certs", BASE_PATH_OIDC + "/login-status-iframe.html", BASE_PATH_OIDC + "/logout"})
    public void forwardOidcGet(HttpServletRequest request, HttpServletResponse response) {
        String path = request.getRequestURI().substring(
                request.getRequestURI().lastIndexOf(BASE_PATH_OIDC) + BASE_PATH_OIDC.length() + 1);
        forward(forwardTokenPath + path, request, response);
    }

    /**
     * Forwards certain OIDC POST requests to the appropriate Keycloak endpoints
     */
    @PostMapping({BASE_PATH_OIDC + "/auth", BASE_PATH_OIDC + "/token", BASE_PATH_OIDC + "/token/introspect",
            BASE_PATH_OIDC +"/userinfo"})
    public void forwardOidcPost(HttpServletRequest request, HttpServletResponse response) {
        forwardOidcGet(request, response);
    }

    private void forward(String forwardPath, HttpServletRequest request, HttpServletResponse response) {
        try {
            RequestDispatcher rd = request.getRequestDispatcher(forwardPath);
            rd.forward(request, response);
        } catch (ServletException | IOException se) {
            throw new ForwardException("Error forwarding request " + request.getMethod() + " " + request.getContextPath(), se);
        }
    }

}

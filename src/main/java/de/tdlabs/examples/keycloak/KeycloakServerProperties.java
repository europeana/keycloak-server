package de.tdlabs.examples.keycloak;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Keycloak related configuration
 * @author Thomas Darimont
 */
@ConfigurationProperties(prefix = "keycloak.server")
public class KeycloakServerProperties {

    private String contextPath;
    private String pepper;
    private AdminUser adminUser = new AdminUser();

    public String getContextPath() {
        return contextPath;
    }

    public void setContextPath(String contextPath) {
        this.contextPath = contextPath;
    }

    public String getPepper(){
        return pepper;
    }

    public void setPepper(String pepper) {
        this.pepper = pepper;
    }

    public AdminUser getAdminUser() {
        return this.adminUser;
    }

    public static class AdminUser {

        String username;
        String password;

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return this.password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

    }
}

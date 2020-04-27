package eu.europeana.keycloak;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.boot.bind.PropertiesConfigurationFactory;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.validation.BindException;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.util.Properties;

/**
 * Utility for loading property files before Spring is initialized and making this available in the
 * KeycloakServerProperties
 *
 * Created by luthien on 23/01/2020.
 * Modified by Patrick Ehlert on Feb 5 2020
 */
public final class StaticPropertyUtil {

    private static final Logger LOG = LogManager.getLogger(StaticPropertyUtil.class);
    private static final KeycloakServerProperties kcProperties = getKcProperties();

    private StaticPropertyUtil(){
        // empty constructor to avoid initialization
    }

    public static String getPepper(){
        return kcProperties.getPepper();
    }

    public static String getContextPath(){
        return kcProperties.getContextPath();
    }

    public static KeycloakServerProperties.AdminUser getAdminUser() {
        return kcProperties.getAdminUser();
    }

    private static KeycloakServerProperties getKcProperties() {
        try {
            Properties appProps = loadProperties("keycloak.properties", true);
            Properties userProps = loadProperties("keycloak-user.properties", false);

            MutablePropertySources propertySources = new MutablePropertySources();
            propertySources.addFirst(new PropertiesPropertySource("kcProperties", appProps));
            if (userProps != null) {
                propertySources.addLast(new PropertiesPropertySource("kcProperties", userProps));
            }

            KeycloakServerProperties kcProperties = new KeycloakServerProperties();

            PropertiesConfigurationFactory<KeycloakServerProperties> configurationFactory = new PropertiesConfigurationFactory<>(kcProperties);
            configurationFactory.setPropertySources(propertySources);
            configurationFactory.setTargetName("keycloak");
            configurationFactory.bindPropertiesToTarget();

            return kcProperties;

        } catch (IOException | BindException e) {
            throw new IllegalArgumentException(e);

        }
    }

    private static Properties loadProperties(String fileName, boolean mandatory) throws IOException {
        LOG.info("Loading {}...", fileName);
        URL resource = Thread.currentThread().getContextClassLoader().getResource(fileName);
        if (resource == null) {
            if (mandatory) {
                throw new FileNotFoundException(fileName + " not found!");
            }
            return null;
        }
        Properties props = new Properties();
        try (FileInputStream fis = new FileInputStream(resource.getFile())) {
            props.load(fis);
        }
        return props;
    }

}

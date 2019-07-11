package eu.europeana.keycloak.themes;

import org.keycloak.Config;
import org.keycloak.common.Version;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.theme.Theme;
import org.keycloak.theme.ThemeSelectorProvider;

import static org.keycloak.theme.DefaultThemeSelectorProvider.LOGIN_THEME_KEY;

public class EuropeanaSelectorProvider implements ThemeSelectorProvider {

    private final KeycloakSession session;

    public EuropeanaSelectorProvider(KeycloakSession session) {
        this.session = session;
    }


    @Override
    public String getThemeName(Theme.Type type) {
        String name = null;

        switch (type) {
            case WELCOME:
                name = Config.scope("theme").get("welcomeTheme");
                break;
            case LOGIN:
                ClientModel client = session.getContext().getClient();
                if (client != null) {
                    name = client.getAttribute(LOGIN_THEME_KEY);
                }

                if (name == null || name.isEmpty()) {
                    name = session.getContext().getRealm().getLoginTheme();
                }
                break;
            case ACCOUNT:
                name = session.getContext().getRealm().getAccountTheme();
                break;
            case EMAIL:
                name = session.getContext().getRealm().getEmailTheme();
                break;
            case ADMIN:
                name = session.getContext().getRealm().getAdminTheme();
                break;
        }

        if (name == null || name.isEmpty()) {
            name = Config.scope("theme").get("default", Version.NAME.toLowerCase());
        }

        return name;
    }

    @Override
    public void close() {
    }
}

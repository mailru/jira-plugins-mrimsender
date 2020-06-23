package ru.mail.jira.plugins.myteam.configuration;

import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.security.xsrf.RequiresXsrfCheck;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.plugin.PluginParseException;
import com.atlassian.plugin.web.ContextProvider;
import org.apache.commons.lang3.StringUtils;
import ru.mail.jira.plugins.commons.RestUtils;

import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.HashMap;
import java.util.Map;

@Produces({MediaType.APPLICATION_JSON})
@Path("/preferences")
public class MyteamProfilePanel implements ContextProvider {
    private final JiraAuthenticationContext jiraAuthenticationContext;
    private final UserData userData;
    private final PluginData pluginData;

    public MyteamProfilePanel(JiraAuthenticationContext jiraAuthenticationContext, UserData userData, PluginData pluginData) {
        this.jiraAuthenticationContext = jiraAuthenticationContext;
        this.userData = userData;
        this.pluginData = pluginData;
    }

    @Override
    public void init(Map<String, String> paramMap) throws PluginParseException {
    }

    @Override
    public Map<String, Object> getContextMap(Map<String, Object> paramMap) {
        ApplicationUser user = jiraAuthenticationContext.getLoggedInUser();
        Map<String, Object> result = new HashMap<String, Object>();
        result.put("mrimLogin", userData.getMrimLogin(user));
        result.put("enabled", userData.isEnabled(user));
        result.put("botName", pluginData.getBotName());
        result.put("botLink", pluginData.getBotLink());
        return result;
    }

    @RequiresXsrfCheck
    @POST
    public Response updateMrimEnabled(@FormParam("mrim_login") final String mrimLogin,
                                      @FormParam("enabled") final boolean enabled) {

        if (enabled && StringUtils.isBlank(mrimLogin))
            throw new IllegalArgumentException(jiraAuthenticationContext.getI18nHelper().getText("ru.mail.jira.plugins.myteam.profilePanel.mrimLoginMustBeSpecified"));

        ApplicationUser user = jiraAuthenticationContext.getLoggedInUser();
        userData.setMrimLogin(user, StringUtils.defaultString(mrimLogin).trim());
        userData.setEnabled(user, enabled);

        return RestUtils.success(null);
    }
}

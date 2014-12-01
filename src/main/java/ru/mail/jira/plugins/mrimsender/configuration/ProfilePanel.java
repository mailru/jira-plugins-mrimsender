package ru.mail.jira.plugins.mrimsender.configuration;

import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.security.xsrf.RequiresXsrfCheck;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.plugin.PluginParseException;
import com.atlassian.plugin.web.ContextProvider;
import org.apache.commons.lang3.StringUtils;
import ru.mail.jira.plugins.commons.RestExecutor;
import ru.mail.jira.plugins.mrimsender.protocol.UserSearcher;

import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.HashMap;
import java.util.Map;

@Produces({ MediaType.APPLICATION_JSON })
@Path("/preferences")
public class ProfilePanel implements ContextProvider {
    private final JiraAuthenticationContext jiraAuthenticationContext;
    private final UserData userData = new UserData();

    public ProfilePanel(JiraAuthenticationContext jiraAuthenticationContext) {
        this.jiraAuthenticationContext = jiraAuthenticationContext;
    }

    @Override
    public void init(Map<String, String> paramMap) throws PluginParseException {
    }

    @Override
    public Map<String, Object> getContextMap(Map<String, Object> paramMap) {
        ApplicationUser user = jiraAuthenticationContext.getUser();
        Map<String, Object> result = new HashMap<String, Object>();
        result.put("mrimLogin", userData.getMrimLogin(user));
        result.put("enabled", userData.isEnabled(user));
        return result;
    }

    @RequiresXsrfCheck
    @POST
    public Response updateCfo(@FormParam("mrim_login") final String mrimLogin,
                              @FormParam("enabled") final boolean enabled) {
        return new RestExecutor<Void>() {
            @Override
            protected Void doAction() {
                if (enabled && StringUtils.isBlank(mrimLogin))
                    throw new IllegalArgumentException(jiraAuthenticationContext.getI18nHelper().getText("ru.mail.jira.plugins.mrimsender.profilePanel.mrimLoginMustBeSpecified"));

                ApplicationUser user = jiraAuthenticationContext.getUser();
                userData.setMrimLogin(user, StringUtils.defaultString(mrimLogin).trim());
                userData.setEnabled(user, enabled);
                UserSearcher.INSTANCE.updateUser(user);
                return null;
            }
        }.getResponse();
    }
}

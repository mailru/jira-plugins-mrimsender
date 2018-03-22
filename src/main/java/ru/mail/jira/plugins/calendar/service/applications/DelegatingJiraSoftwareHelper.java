package ru.mail.jira.plugins.calendar.service.applications;

import com.atlassian.event.api.EventListener;
import com.atlassian.event.api.EventPublisher;
import com.atlassian.greenhopper.api.customfield.ManagedCustomFieldsService;
import com.atlassian.greenhopper.api.issuetype.ManagedIssueTypesService;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.issue.issuetype.IssueType;
import com.atlassian.plugin.ModuleDescriptor;
import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.PluginAccessor;
import com.atlassian.plugin.event.events.PluginEnabledEvent;
import com.atlassian.plugin.spring.scanner.annotation.export.ExportAsService;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.sal.api.lifecycle.LifecycleAware;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@ExportAsService(LifecycleAware.class)
public class DelegatingJiraSoftwareHelper implements LifecycleAware, JiraSoftwareHelper {
    private static final String GREENHOPPER_KEY = "com.pyxis.greenhopper.jira";

    private final Logger logger = LoggerFactory.getLogger(DelegatingJiraSoftwareHelper.class);
    private final EventPublisher eventPublisher;
    private final PluginAccessor pluginAccessor;

    private JiraSoftwareHelper delegate;

    @Autowired
    public DelegatingJiraSoftwareHelper(@ComponentImport EventPublisher eventPublisher, @ComponentImport PluginAccessor pluginAccessor) {
        this.eventPublisher = eventPublisher;
        this.pluginAccessor = pluginAccessor;
        this.eventPublisher.register(this);
    }

    @EventListener
    public void onEvent(PluginEnabledEvent pluginEnabledEvent) {
        Plugin plugin = pluginEnabledEvent.getPlugin();
        if (GREENHOPPER_KEY.equals(plugin.getKey())) {
            logger.info("updating jsw helper delegate");
            createDelegate(plugin);
        }
    }

    @Override
    public void onStart() {
        createDelegate(pluginAccessor.getPlugin(GREENHOPPER_KEY));
    }

    @Override
    public void onStop() {
        this.eventPublisher.unregister(this);
        logger.info("JSW helper destroyed");
    }

    private void createDelegate(Plugin plugin) {
        try {
            Class.forName("com.atlassian.greenhopper.api.issuetype.ManagedIssueTypesService"); //check if greenhopper classes are available
            logger.info("Creating JSW helper");
            ModuleDescriptor<?> managedIssueTypeSerivceDescriptor = plugin.getModuleDescriptor("greenhopper-managedissuetypes-service");
            ModuleDescriptor<?> managedCustomFieldsServiceDescriptor = plugin.getModuleDescriptor("greenhopper-managedcustomfields-service");

            if (managedCustomFieldsServiceDescriptor != null && managedIssueTypeSerivceDescriptor != null) {
                this.delegate = new JiraSoftwareHelperImpl(
                    (ManagedIssueTypesService) managedIssueTypeSerivceDescriptor.getModule(),
                    (ManagedCustomFieldsService) managedCustomFieldsServiceDescriptor.getModule()
                );
            } else {
                logger.warn("JSW modules not available");
            }
        } catch (ClassNotFoundException e) {
            logger.info("Creating JSW stub helper");
            this.delegate = new JiraSoftwareHelperStub();
        }
    }

    @Override
    public boolean isAvailable() {
        return delegate.isAvailable();
    }

    @Override
    public CustomField getEpicLinkField() {
        return delegate.getEpicLinkField();
    }

    @Override
    public CustomField getRankField() {
        return delegate.getRankField();
    }

    @Override
    public IssueType getEpicIssueType() {
        return delegate.getEpicIssueType();
    }
}

package ru.mail.jira.plugins.calendar.upgrades.v6;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.activeobjects.external.ActiveObjectsUpgradeTask;
import com.atlassian.activeobjects.external.ModelVersion;
import com.atlassian.sal.api.transaction.TransactionCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mail.jira.plugins.calendar.model.*;

public class Version6UpgradeTask implements ActiveObjectsUpgradeTask {
    private final static Logger log = LoggerFactory.getLogger(Version6UpgradeTask.class);

    @Override
    public ModelVersion getModelVersion() {
        return ModelVersion.valueOf("6");
    }

    @Override
    public void upgrade(ModelVersion modelVersion, ActiveObjects ao) {
        log.info("Current version " + modelVersion.toString());
        ao.migrate(Calendar.class);
        ao.executeInTransaction(new TransactionCallback<Void>() {
            @Override
            public Void doInTransaction() {
                for (Calendar calendar : ao.find(Calendar.class)) {
                    calendar.setCanCreateEvents(true);
                    calendar.save();
                }
                return null;
            }
        });
    }
}

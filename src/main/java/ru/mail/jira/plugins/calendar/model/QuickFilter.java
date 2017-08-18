package ru.mail.jira.plugins.calendar.model;

import net.java.ao.Entity;
import net.java.ao.ManyToMany;
import net.java.ao.OneToMany;
import net.java.ao.schema.StringLength;

public interface QuickFilter extends Entity {
    String getCreatorKey();
    void setCreatorKey(String creatorKey);

    int getCalendarId();
    void setCalendarId(int calendarId);

    String getName();
    void setName(String name);

    @StringLength(StringLength.UNLIMITED)
    String getJql();
    void setJql(String jql);

    @StringLength(StringLength.UNLIMITED)
    String getDescription();
    void setDescription(String description);

    boolean isShare();
    void setShare(boolean share);

    @OneToMany
    FavouriteQuickFilter[] getFavouriteQuickFilters();
}

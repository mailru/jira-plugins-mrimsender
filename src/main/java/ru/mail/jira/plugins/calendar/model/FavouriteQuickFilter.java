package ru.mail.jira.plugins.calendar.model;

import net.java.ao.Entity;
import net.java.ao.schema.Table;

@Table("FAVOURITE_FILTERS")
public interface FavouriteQuickFilter extends Entity {
    QuickFilter getQuickFilter();
    void setQuickFilter(QuickFilter quickFilter);

    UserCalendar getUserCalendar();
    void setUserCalendar(UserCalendar userCalendar);

    boolean isSelected();
    void setSelected(boolean selected);
}

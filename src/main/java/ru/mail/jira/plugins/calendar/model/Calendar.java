package ru.mail.jira.plugins.calendar.model;

import net.java.ao.Entity;
import net.java.ao.OneToMany;

/**
 * User calendar preference
 */
public interface Calendar extends Entity {
    /** Name */
    String getName();
    void setName(String name);

    /** Author */
    String getAuthorKey();
    void setAuthorKey(String authorKey);

    /** Color */
    String getColor();
    void setColor(String color);

    /** Source: Project or filter */
    String getSource();
    void setSource(String source);

    /** Project roles and groups */
    @OneToMany
    Share[] getShares();

    /** First date field of range */
    String getEventStart();
    void setEventStart(String eventStart);

    /** Last date field of range */
    String getEventEnd();
    void setEventEnd(String eventEnd);

    /** Fields of calendar*/
    String getDisplayedFields();
    void setDisplayedFields(String displayedFields);
}
package ru.mail.jira.plugins.calendar.model;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@SuppressWarnings({"UnusedDeclaration", "FieldCanBeLocal"})
@XmlRootElement
public class CalendarOutput {
    @XmlElement
    private int id;
    @XmlElement
    private String name;
    @XmlElement
    private String color;
    @XmlElement
    private boolean changable;
    @XmlElement
    private boolean visible;
    @XmlElement
    private boolean isMy;
    @XmlElement
    private boolean fromOthers;
    @XmlElement
    private boolean hasError;
    @XmlElement
    private String error;

    public CalendarOutput() { }

    public CalendarOutput(Calendar calendar) {
        this.id = calendar.getID();
        this.name = calendar.getName();
        this.color = calendar.getColor();
    }

    public void setChangable(boolean changable) {
        this.changable = changable;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public void setIsMy(boolean isMy) {
        this.isMy = isMy;
    }

    public void setFromOthers(boolean fromOthers) {
        this.fromOthers = fromOthers;
    }

    public void setHasError(boolean withErrors) {
        this.hasError = withErrors;
    }

    public void setError(String error) {
        this.error = error;
    }
}

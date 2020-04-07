package ru.mail.jira.plugins.mrimsender.icq.dto.parts;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

public class Mention extends Part<Mention.Data> {

    public String getUserId() {
        return this.getPayload().userId;
    }

    public String getFirstName() {
        return this.getPayload().firstName;
    }

    public String getLastName() {
        return this.getPayload().lastName;
    }

    @Getter
    @Setter
    @ToString
    public static class Data {
        private String userId;
        private String firstName;
        private String lastName;
    }
}

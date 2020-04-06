package ru.mail.jira.plugins.mrimsender.icq.dto.parts;

import org.codehaus.jackson.map.annotate.JsonDeserialize;

@JsonDeserialize(as = Mention.class)
public class Mention extends Part<Mention.Data> {
    public static class Data {
        public String userId;
        public String firstName;
        public String lastName;

        @Override
        public String toString() {
            return "Data{" +
                    "userId='" + userId + '\'' +
                    ", firstName='" + firstName + '\'' +
                    ", lastName='" + lastName + '\'' +
                    '}';
        }
    }

    public String getUserId() {
        return this.getPayload().userId;
    }

    public String getFirstName() {
        return this.getPayload().firstName;
    }

    public String getLastName() {
        return this.getPayload().lastName;
    }

    @Override
    public String toString() {
        return super.toString();
    }
}

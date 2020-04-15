package ru.mail.jira.plugins.mrimsender.icq.dto.parts;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;

public class File extends Part<File.Data> {
    public String getFileId() {
        return this.getPayload().fileId;
    }

    @Getter
    @Setter
    @ToString
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Data {
        private String fileId;
    }
}

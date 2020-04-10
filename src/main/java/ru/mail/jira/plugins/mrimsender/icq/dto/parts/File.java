package ru.mail.jira.plugins.mrimsender.icq.dto.parts;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

public class File extends Part<File.Data> {
    public String getFileId() {
        return this.getPayload().fileId;
    }

    @Getter
    @Setter
    @ToString
    public static class Data {
        private String fileId;
    }
}

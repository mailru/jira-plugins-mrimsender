package ru.mail.jira.plugins.calendar.util;

import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.map.JsonSerializer;
import org.codehaus.jackson.map.SerializerProvider;

import java.io.IOException;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.SignStyle;

import static java.time.temporal.ChronoField.HOUR_OF_DAY;
import static java.time.temporal.ChronoField.MINUTE_OF_HOUR;

public class LocalTimeSerializer extends JsonSerializer<LocalTime> {
    private static final DateTimeFormatter FORMATTER = new DateTimeFormatterBuilder()
                .appendValue(HOUR_OF_DAY, 1, 2, SignStyle.NEVER)
                .appendLiteral(':')
                .appendValue(MINUTE_OF_HOUR, 2)
                .toFormatter();

    @Override
    public void serialize(LocalTime value, JsonGenerator jgen, SerializerProvider provider) throws IOException {
        provider.defaultSerializeValue(FORMATTER.format(value), jgen);
    }
}

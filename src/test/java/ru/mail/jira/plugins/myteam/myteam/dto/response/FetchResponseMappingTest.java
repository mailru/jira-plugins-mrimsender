package ru.mail.jira.plugins.myteam.myteam.dto.response;

import org.junit.Test;
import ru.mail.jira.plugins.commons.JacksonObjectMapper;

import static org.junit.Assert.assertNotNull;

public class FetchResponseMappingTest {

    @Test
    public void testSuccessMapping() {
        // GIVEN
        JacksonObjectMapper jacksonObjectMapper = new JacksonObjectMapper();
        String fullEventJsonFromTeams = getFullEventJsonFromTeams();

        // WHEN
        FetchResponse fetchResponse = jacksonObjectMapper.readValue(fullEventJsonFromTeams, FetchResponse.class);

        // THEN
        assertNotNull(fetchResponse);
    }

    private String getFullEventJsonFromTeams() {
        return "{\n" +
                "  \"events\": [\n" +
                "    {\n" +
                "      \"eventId\": 1,\n" +
                "      \"type\": \"newMessage\",\n" +
                "      \"payload\": {\n" +
                "        \"msgId\": \"57883346846815032\",\n" +
                "        \"chat\": {\n" +
                "          \"chatId\": \"681869378@chat.agent\",\n" +
                "          \"type\": \"channel\",\n" +
                "          \"title\": \"The best channel\"\n" +
                "        },\n" +
                "        \"from\": {\n" +
                "          \"userId\": \"1234567890\",\n" +
                "          \"firstName\": \"Name\",\n" +
                "          \"lastName\": \"SurName\"\n" +
                "        },\n" +
                "        \"timestamp\": 1546290000,\n" +
                "        \"text\": \"Hello!\",\n" +
                "        \"format\": {\n" +
                "          \"bold\": [\n" +
                "            {\n" +
                "              \"offset\": 0,\n" +
                "              \"length\": 1\n" +
                "            }\n" +
                "          ],\n" +
                "          \"italic\": [\n" +
                "            {\n" +
                "              \"offset\": 0,\n" +
                "              \"length\": 1\n" +
                "            }\n" +
                "          ],\n" +
                "          \"underline\": [\n" +
                "            {\n" +
                "              \"offset\": 0,\n" +
                "              \"length\": 1\n" +
                "            }\n" +
                "          ],\n" +
                "          \"strikethrough\": [\n" +
                "            {\n" +
                "              \"offset\": 0,\n" +
                "              \"length\": 1\n" +
                "            }\n" +
                "          ],\n" +
                "          \"link\": [\n" +
                "            {\n" +
                "              \"offset\": 0,\n" +
                "              \"length\": 1,\n" +
                "              \"url\": \"https://example.com/\"\n" +
                "            }\n" +
                "          ],\n" +
                "          \"mention\": [\n" +
                "            {\n" +
                "              \"offset\": 0,\n" +
                "              \"length\": 1\n" +
                "            }\n" +
                "          ],\n" +
                "          \"inline_code\": [\n" +
                "            {\n" +
                "              \"offset\": 0,\n" +
                "              \"length\": 1\n" +
                "            }\n" +
                "          ],\n" +
                "          \"pre\": [\n" +
                "            {\n" +
                "              \"offset\": 0,\n" +
                "              \"length\": 1,\n" +
                "              \"code\": \"string\"\n" +
                "            }\n" +
                "          ],\n" +
                "          \"ordered_list\": [\n" +
                "            {\n" +
                "              \"offset\": 0,\n" +
                "              \"length\": 1\n" +
                "            }\n" +
                "          ],\n" +
                "          \"unordered_list\": [\n" +
                "            {\n" +
                "              \"offset\": 0,\n" +
                "              \"length\": 1\n" +
                "            }\n" +
                "          ],\n" +
                "          \"quote\": [\n" +
                "            {\n" +
                "              \"offset\": 0,\n" +
                "              \"length\": 1\n" +
                "            }\n" +
                "          ]\n" +
                "        },\n" +
                "        \"parts\": [\n" +
                "          {\n" +
                "            \"type\": \"reply\",\n" +
                "            \"payload\": {\n" +
                "              \"message\": {\n" +
                "                \"from\": {\n" +
                "                  \"firstName\": \"Name\",\n" +
                "                  \"lastName\": \"SurName\",\n" +
                "                  \"userId\": \"1234567890\"\n" +
                "                },\n" +
                "                \"msgId\": \"6724238139063271643\",\n" +
                "                \"text\": \"some text\",\n" +
                "                \"format\": {\n" +
                "                  \"bold\": [\n" +
                "                    {\n" +
                "                      \"offset\": 0,\n" +
                "                      \"length\": 1\n" +
                "                    }\n" +
                "                  ],\n" +
                "                  \"italic\": [\n" +
                "                    {\n" +
                "                      \"offset\": 0,\n" +
                "                      \"length\": 1\n" +
                "                    }\n" +
                "                  ],\n" +
                "                  \"underline\": [\n" +
                "                    {\n" +
                "                      \"offset\": 0,\n" +
                "                      \"length\": 1\n" +
                "                    }\n" +
                "                  ],\n" +
                "                  \"strikethrough\": [\n" +
                "                    {\n" +
                "                      \"offset\": 0,\n" +
                "                      \"length\": 1\n" +
                "                    }\n" +
                "                  ],\n" +
                "                  \"link\": [\n" +
                "                    {\n" +
                "                      \"offset\": 0,\n" +
                "                      \"length\": 1,\n" +
                "                      \"url\": \"https://example.com/\"\n" +
                "                    }\n" +
                "                  ],\n" +
                "                  \"mention\": [\n" +
                "                    {\n" +
                "                      \"offset\": 0,\n" +
                "                      \"length\": 1\n" +
                "                    }\n" +
                "                  ],\n" +
                "                  \"inline_code\": [\n" +
                "                    {\n" +
                "                      \"offset\": 0,\n" +
                "                      \"length\": 1\n" +
                "                    }\n" +
                "                  ],\n" +
                "                  \"pre\": [\n" +
                "                    {\n" +
                "                      \"offset\": 0,\n" +
                "                      \"length\": 1,\n" +
                "                      \"code\": \"string\"\n" +
                "                    }\n" +
                "                  ],\n" +
                "                  \"ordered_list\": [\n" +
                "                    {\n" +
                "                      \"offset\": 0,\n" +
                "                      \"length\": 1\n" +
                "                    }\n" +
                "                  ],\n" +
                "                  \"unordered_list\": [\n" +
                "                    {\n" +
                "                      \"offset\": 0,\n" +
                "                      \"length\": 1\n" +
                "                    }\n" +
                "                  ],\n" +
                "                  \"quote\": [\n" +
                "                    {\n" +
                "                      \"offset\": 0,\n" +
                "                      \"length\": 1\n" +
                "                    }\n" +
                "                  ]\n" +
                "                },\n" +
                "                \"timestamp\": 1565608694\n" +
                "              }\n" +
                "            }\n" +
                "          }\n" +
                "        ]\n" +
                "      }\n" +
                "    }\n" +
                "  ]\n" +
                "}";
    }

}
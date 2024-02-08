/* (C)2024 */
package ru.mail.jira.plugins.myteam.myteam.dto.chats;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import ru.mail.jira.plugins.commons.JacksonObjectMapper;

class MemberTest {

  private static JacksonObjectMapper jacksonObjectMapper;

  @BeforeAll
  static void setUp() {
    jacksonObjectMapper = new JacksonObjectMapper();
  }

  @Test
  void parseJsonWithoutFields() {
    // GIVEN
    String memberJson = "{}";

    // WHEN
    Member member = jacksonObjectMapper.readValue(memberJson, Member.class);

    // THEN
    assertNotNull(member);
    assertNull(member.getUserId());
    assertFalse(member.isCreator());
    assertFalse(member.isAdmin());
  }

  @Test
  void parseJsonWithOnlyUserId() {
    // GIVEN
    String memberJson = "{\n" + "      \"userId\": \"1000000569\"\n" + "    }";

    // WHEN
    Member member = jacksonObjectMapper.readValue(memberJson, Member.class);

    // THEN
    assertNotNull(member);
    assertEquals("1000000569", member.getUserId());
    assertFalse(member.isCreator());
    assertFalse(member.isAdmin());
  }

  @Test
  void parseJsonWithUserIdAndCreatorProps() {
    // GIVEN
    String memberJson =
        "{\n" + "      \"userId\": \"1000000569\",\n" + "      \"creator\": true\n" + "    }";

    // WHEN
    Member member = jacksonObjectMapper.readValue(memberJson, Member.class);

    // THEN
    assertNotNull(member);
    assertEquals("1000000569", member.getUserId());
    assertTrue(member.isCreator());
    assertFalse(member.isAdmin());
  }

  @Test
  void parseJsonWithUserIdAndAdminProps() {
    // GIVEN
    String memberJson =
        "{\n" + "      \"userId\": \"1000000569\",\n" + "      \"admin\": true\n" + "    }";

    // WHEN
    Member member = jacksonObjectMapper.readValue(memberJson, Member.class);

    // THEN
    assertNotNull(member);
    assertEquals("1000000569", member.getUserId());
    assertFalse(member.isCreator());
    assertTrue(member.isAdmin());
  }

  @Test
  void parseJsonWithFullProps() {
    // GIVEN
    String memberJson =
        "{\n"
            + "      \"userId\": \"1000000569\",\n"
            + "      \"admin\": true,\n"
            + "      \"creator\": true\n"
            + "    }";

    // WHEN
    Member member = jacksonObjectMapper.readValue(memberJson, Member.class);

    // THEN
    assertNotNull(member);
    assertEquals("1000000569", member.getUserId());
    assertTrue(member.isCreator());
    assertTrue(member.isAdmin());
  }
}

/* (C)2020 */
package ru.mail.jira.plugins.myteam.commons;

import static java.util.stream.Collectors.joining;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collection;
import java.util.Date;
import java.util.Map;
import java.util.StringJoiner;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.mail.jira.plugins.myteam.bot.events.ChatMessageEvent;
import ru.mail.jira.plugins.myteam.bot.events.MyteamEvent;
import ru.mail.jira.plugins.myteam.myteam.dto.parts.Mention;
import ru.mail.jira.plugins.myteam.myteam.dto.parts.Part;

public class Utils {

  public static String stringifyMap(Map<?, ?> map) {
    if (map == null) return "";
    return map.entrySet().stream()
        .map((entry) -> String.join(" : ", entry.getKey().toString(), entry.getValue().toString()))
        .collect(joining("\n"));
  }

  @NotNull
  public static String unshieldText(@NotNull String s) {
    return s.replaceAll("\\\\([*+#_~\\-`!.<>\\[\\]|(){}\\\\])", "$1");
  }

  @NotNull
  public static String shieldText(@NotNull String str) {
    StringBuilder result = new StringBuilder();
    char[] arrayFromInput = str.toCharArray();
    for (char c : arrayFromInput) {
      switch (c) {
        case '*':
          result.append("\\*");
          break;
        case '_':
          result.append("\\_");
          break;
        case '~':
          result.append("\\~");
          break;
        case '`':
          result.append("\\`");
          break;
        case '-':
          result.append("\\-");
          break;
        case '>':
          result.append("\\>");
          break;
        case '\\':
          result.append("\\\\");
          break;
        case '{':
          result.append("\\{");
          break;
        case '}':
          result.append("\\}");
          break;
        case '[':
          result.append("\\[");
          break;
        case ']':
          result.append("\\]");
          break;
        case '(':
          result.append("\\(");
          break;
        case ')':
          result.append("\\)");
          break;
        case '#':
          result.append("\\#");
          break;
        case '+':
          result.append("\\+");
          break;
        case '.':
          result.append("\\.");
          break;
        case '!':
          result.append("\\!");
          break;
        default:
          result.append(c);
      }
    }
    return result.toString();
  }

  public static String stringifyCollection(Collection<?> collection) {
    StringJoiner sj = new StringJoiner("\n");
    collection.forEach(obj -> sj.add(obj.toString()));
    return sj.toString();
  }

  @Nullable
  public static String removeAllEmojis(@Nullable String str) {
    if (str == null) {
      return null;
    } else {
      return str.replaceAll("[^\\p{L}\\p{M}\\p{N}\\p{P}\\p{Z}\\p{Cf}\\p{Cs}\\s\\|$^+=~±><]", "");
    }
  }

  @Nullable
  public static String getEmailFromMention(MyteamEvent event) {
    String userEmail = null;
    if (event instanceof ChatMessageEvent) {
      ChatMessageEvent chatMessageEvent = (ChatMessageEvent) event;
      if (chatMessageEvent.isHasMentions() && chatMessageEvent.getMessageParts() != null) {
        for (Part part : chatMessageEvent.getMessageParts()) {
          if (part instanceof Mention) {
            userEmail = ((Mention) part).getUserId();
          }
        }
      }
    }
    return userEmail;
  }

  public static LocalDateTime convertToLocalDateTime(Date date) {
    return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
  }

  public static Date convertToDate(LocalDateTime date) {
    return java.util.Date.from(date.atZone(ZoneId.systemDefault()).toInstant());
  }
}

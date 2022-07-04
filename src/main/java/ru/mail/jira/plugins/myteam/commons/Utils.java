/* (C)2020 */
package ru.mail.jira.plugins.myteam.commons;

import static java.util.stream.Collectors.joining;

import java.util.Collection;
import java.util.Map;
import java.util.StringJoiner;
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

  public static String unshieldText(String s) {
    return s.replaceAll("\\\\([*+#_~\\-`!.<>\\[\\]|(){}\\\\])", "$1");
  }

  public static String shieldText(String str) {
    if (str == null) {
      return null;
    }
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

  public static String removeAllEmojis(String str) {
    if (str == null) {
      return null;
    } else {
      return str.replaceAll("[^\\p{L}\\p{M}\\p{N}\\p{P}\\p{Z}\\p{Cf}\\p{Cs}\\s\\|$^+=~±><]", "");
    }
  }

  @Nullable
  public static String getEmailFromMention(MyteamEvent event) {
    String userEmail = null;
    if (event instanceof ChatMessageEvent && ((ChatMessageEvent) event).isHasMentions()) {
      for (Part part : ((ChatMessageEvent) event).getMessageParts()) {
        if (part instanceof Mention) {
          userEmail = ((Mention) part).getUserId();
        }
      }
    }
    return userEmail;
  }
}

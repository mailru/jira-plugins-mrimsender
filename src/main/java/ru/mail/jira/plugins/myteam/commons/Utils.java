/* (C)2020 */
package ru.mail.jira.plugins.myteam.commons;

import static java.util.stream.Collectors.joining;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.Map;
import java.util.StringJoiner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import kong.unirest.UnirestException;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.Nullable;
import ru.mail.jira.plugins.commons.HttpClient;
import ru.mail.jira.plugins.myteam.myteam.dto.parts.Mention;
import ru.mail.jira.plugins.myteam.myteam.dto.parts.Part;
import ru.mail.jira.plugins.myteam.protocol.events.ChatMessageEvent;
import ru.mail.jira.plugins.myteam.protocol.events.MyteamEvent;

public class Utils {

  private static final Pattern pattern =
      Pattern.compile("[A-Z][A-Z\\d]+-[\\d]+", Pattern.CASE_INSENSITIVE);

  /**
   * Finding URL in string
   *
   * @param str - string in which we wiil look for url
   * @param urlPrefix base url for search
   * @return parsed URL object from first found prefix occurrence or null value if first occurrence
   *     couldn't be parsed to url
   */
  @Nullable
  public static URL tryFindUrlByPrefixInStr(String str, String urlPrefix) {
    if (StringUtils.isEmpty(str)) return null;

    int startIndex = str.indexOf(urlPrefix);
    int strLen = str.length();
    if (startIndex == -1) return null;

    int endIndex = startIndex;
    while (endIndex < strLen && !Character.isWhitespace(str.charAt(endIndex))) endIndex++;

    String urlStr = str.substring(startIndex, endIndex);

    try {
      return new URL(urlStr);
    } catch (MalformedURLException e) {
      // this is not very cool because of performance reasons,
      // throwing and catching exceptions is much slower than just check nulls =(
      return null;
    }
  }

  public static InputStream loadUrlFile(String url) throws UnirestException {
    return new ByteArrayInputStream(HttpClient.getPrimaryClient().get(url).asBytes().getBody());
  }

  @Nullable
  public static String findIssueKeyInStr(@Nullable String str) {
    if (str == null) {
      return null;
    }
    Matcher result = pattern.matcher(str);
    return result.find() ? result.group(0) : null;
  }

  public static String stringifyMap(Map<?, ?> map) {
    if (map == null) return "";
    return map.entrySet().stream()
        .map((entry) -> String.join(" : ", entry.getKey().toString(), entry.getValue().toString()))
        .collect(joining("\n"));
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

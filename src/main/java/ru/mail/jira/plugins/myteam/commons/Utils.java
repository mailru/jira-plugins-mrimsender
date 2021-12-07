/* (C)2020 */
package ru.mail.jira.plugins.myteam.commons;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Nullable;
import kong.unirest.UnirestException;
import org.apache.commons.lang.StringUtils;
import ru.mail.jira.plugins.commons.HttpClient;

public class Utils {

  private static final Pattern pattern =
      Pattern.compile("[\\w\\d]+-[\\d]+", Pattern.CASE_INSENSITIVE);

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
  public static String findIssueKeyInStr(String str) {
    Matcher result = pattern.matcher(str);
    return result.find() ? result.group(0) : null;
  }
}

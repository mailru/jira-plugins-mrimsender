/* (C)2020 */
package ru.mail.jira.plugins.myteam;

import java.net.MalformedURLException;
import java.net.URL;
import javax.annotation.Nullable;
import org.apache.commons.lang.StringUtils;

public class Utils {
  /**
   * @param str - string in which we wiil look for url
   * @param urlPrefix
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
}

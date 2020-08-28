package ru.mail.jira.plugins.myteam;

import javax.annotation.Nullable;
import java.net.MalformedURLException;
import java.net.URL;

public class Utils {
    /**
     * @param str - string in which we wiil look for url
     * @param urlPrefix
     * @return parsed URL object from first found prefix occurrence
     * or null value if first occurrence couldn't be parsed to url
     */
    @Nullable
    public static URL tryFindUrlByPrefixInStr(String str, String urlPrefix) {
        int startIndex = str.indexOf(urlPrefix);
        int strLen = str.length();
        if ( startIndex == -1)
            return null;

        int endIndex = startIndex;
        while (endIndex < strLen && !Character.isWhitespace(str.charAt(endIndex)))
            endIndex++;
        String urlStr = str.substring(startIndex, endIndex + 1);

        try {
            return new URL(urlStr);
        } catch (MalformedURLException e) {
            return null;
        }
    }
}

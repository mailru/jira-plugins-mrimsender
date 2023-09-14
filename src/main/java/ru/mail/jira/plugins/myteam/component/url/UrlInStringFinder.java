/* (C)2023 */
package ru.mail.jira.plugins.myteam.component.url;

import static java.util.Collections.emptyList;
import static java.util.Collections.unmodifiableList;
import static org.apache.commons.lang.StringUtils.isEmpty;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;
import ru.mail.jira.plugins.myteam.component.url.dto.Link;

@Component
public class UrlInStringFinder implements UrlFinder<List<Link>, String> {

  private static final String LINKS_REGEX =
      "([A-Za-z][A-Za-z0-9+.-]{1,120}:"
          + "[A-Za-z0-9/](([A-Za-z0-9$_.+!*,;/?:@&~=-])|%[A-Fa-f0-9]{2}){1,333}"
          + "(#([a-zA-Z0-9][a-zA-Z0-9$_.+!*,;/?:@&~=%-]{0,1000}))?)";
  private static final Pattern LINKS_PATTERN =
      Pattern.compile(LINKS_REGEX, Pattern.CASE_INSENSITIVE + Pattern.MULTILINE);

  @Override
  public List<Link> findUrls(final String source) {
    if (isEmpty(source)) {
      return emptyList();
    }

    final List<Link> unmaskedLinks = new ArrayList<>();
    final Matcher matcher = LINKS_PATTERN.matcher(source);
    while (matcher.find()) {
      final String unmaskedLink = matcher.group();
      unmaskedLinks.add(Link.of(unmaskedLink, unmaskedLink, false));
    }
    return unmodifiableList(unmaskedLinks);
  }
}

/* (C)2023 */
package ru.mail.jira.plugins.myteam.component.url;

import java.util.List;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import ru.mail.jira.plugins.myteam.component.url.dto.Link;
import ru.mail.jira.plugins.myteam.myteam.dto.TextFormatMetadata;

@RequiredArgsConstructor
public abstract class AbstractUrlFinder<T, R> implements UrlFinder<T, R> {
  private final UrlInStringFinder urlInStringFinder;

  protected int calcEndOffsetOfUrl(final TextFormatMetadata.Link maskedLinkMetadata) {
    return maskedLinkMetadata.getOffset() + maskedLinkMetadata.getLength();
  }

  protected List<Link> findUnmaskedUrls(final Supplier<String> textWithLinkProvider) {
    return urlInStringFinder.findUrls(textWithLinkProvider.get());
  }
}

/* (C)2023 */
package ru.mail.jira.plugins.myteam.component.url;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toUnmodifiableList;

import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import ru.mail.jira.plugins.myteam.bot.events.ChatMessageEvent;
import ru.mail.jira.plugins.myteam.component.url.dto.Link;
import ru.mail.jira.plugins.myteam.component.url.dto.LinksInMessage;
import ru.mail.jira.plugins.myteam.myteam.dto.EmptyFormatMetadata;
import ru.mail.jira.plugins.myteam.myteam.dto.TextFormatMetadata;

@Component
public class UrlFinderInMainEvent extends AbstractUrlFinder<LinksInMessage, ChatMessageEvent> {

  public UrlFinderInMainEvent(final UrlInStringFinder urlInStringFinder) {
    super(urlInStringFinder);
  }

  @Override
  public LinksInMessage findUrls(ChatMessageEvent source) {
    if (source.getFormat() == null || source.getFormat() instanceof EmptyFormatMetadata) {
      return LinksInMessage.of(super.findUnmaskedUrls(source::getMessage));
    }

    return findAllLinks(source);
  }

  private LinksInMessage findAllLinks(final ChatMessageEvent source) {
    return LinksInMessage.of(
        Stream.concat(
                super.findUnmaskedUrls(source::getMessage).stream(),
                Optional.ofNullable(source.getFormat().getLink()).orElse(emptyList()).stream()
                    .filter(Objects::nonNull)
                    .map(maskedLinkMetadata -> mapLinkMetadataToLink(maskedLinkMetadata, source)))
            .collect(toUnmodifiableList()));
  }

  private Link mapLinkMetadataToLink(
      @NotNull final TextFormatMetadata.Link maskedLinkMetadata, final ChatMessageEvent source) {
    return Link.of(
        Optional.ofNullable(maskedLinkMetadata.getUrl()).orElse(""),
        source
            .getMessage()
            .substring(maskedLinkMetadata.getOffset(), calcEndOffsetOfUrl(maskedLinkMetadata)),
        true);
  }
}

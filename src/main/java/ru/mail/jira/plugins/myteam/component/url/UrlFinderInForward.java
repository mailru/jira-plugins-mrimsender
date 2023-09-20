/* (C)2023 */
package ru.mail.jira.plugins.myteam.component.url;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toUnmodifiableList;

import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import ru.mail.jira.plugins.myteam.component.url.dto.Link;
import ru.mail.jira.plugins.myteam.component.url.dto.LinksInMessage;
import ru.mail.jira.plugins.myteam.myteam.dto.TextFormatMetadata;
import ru.mail.jira.plugins.myteam.myteam.dto.parts.Forward;

@Component
public class UrlFinderInForward extends AbstractUrlFinder<LinksInMessage, Forward> {

  public UrlFinderInForward(UrlInStringFinder urlInStringFinder) {
    super(urlInStringFinder);
  }

  @Override
  public LinksInMessage findUrls(final Forward source) {
    return Optional.ofNullable(source.getMessage().getFormat())
        .map(textFormatMetadata -> findAllLinks(source))
        .orElse(LinksInMessage.of(super.findUnmaskedUrls(() -> source.getMessage().getText())));
  }

  private LinksInMessage findAllLinks(final Forward source) {
    return LinksInMessage.of(
        Stream.concat(
                super.findUnmaskedUrls(() -> source.getMessage().getText()).stream(),
                Optional.ofNullable(source.getMessage().getFormat().getLink())
                    .orElse(emptyList())
                    .stream()
                    .filter(Objects::nonNull)
                    .map(maskedLinkMetadata -> mapLinkMetadataToLink(maskedLinkMetadata, source)))
            .collect(toUnmodifiableList()));
  }

  private Link mapLinkMetadataToLink(
      @NotNull final TextFormatMetadata.Link maskedLinkMetadata, final Forward forward) {
    return Link.of(
        Optional.ofNullable(maskedLinkMetadata.getUrl()).orElse(""),
        forward
            .getMessage()
            .getText()
            .substring(maskedLinkMetadata.getOffset(), calcEndOffsetOfUrl(maskedLinkMetadata)),
        true);
  }
}

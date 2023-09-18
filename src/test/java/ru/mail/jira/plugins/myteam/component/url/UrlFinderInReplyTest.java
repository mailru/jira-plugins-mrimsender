/* (C)2023 */
package ru.mail.jira.plugins.myteam.component.url;

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.function.Predicate;
import org.junit.Test;
import org.mockito.Mockito;
import ru.mail.jira.plugins.myteam.component.url.dto.Link;
import ru.mail.jira.plugins.myteam.component.url.dto.LinksInMessage;
import ru.mail.jira.plugins.myteam.myteam.dto.TextFormatMetadata;
import ru.mail.jira.plugins.myteam.myteam.dto.parts.Reply;

@SuppressWarnings("NullAway")
public class UrlFinderInReplyTest {

  @Test
  public void findUrlsWhenMessageFormatForLinksIsNull() {
    // GIVEN
    Reply source = createForward("some text", null);

    UrlInStringFinder urlInStringFinder = Mockito.mock(UrlInStringFinder.class);
    UrlFinderInReply urlFinderInForward = new UrlFinderInReply(urlInStringFinder);

    // WHEN
    LinksInMessage urls = urlFinderInForward.findUrls(source);

    // THEN
    assertTrue(urls.getLinks().isEmpty());
  }

  @Test
  public void findUrlsWhenMessageTextHasOnlyUnmask() {
    // GIVEN
    Reply source = createForward("http://link.domen", null);
    UrlInStringFinder urlInStringFinder = Mockito.mock(UrlInStringFinder.class);
    UrlFinderInReply urlFinderInForward = new UrlFinderInReply(urlInStringFinder);
    when(urlInStringFinder.findUrls("http://link.domen"))
        .thenReturn(List.of(Link.of("http://link.domen", "http://link.domen", false)));
    // WHEN

    LinksInMessage urls = urlFinderInForward.findUrls(source);

    // THEN
    assertFalse(urls.getLinks().isEmpty());
    assertEquals("http://link.domen", urls.getLinks().get(0).getLink());
    assertEquals("http://link.domen", urls.getLinks().get(0).getMask());
    assertFalse(urls.getLinks().get(0).isMasked());
  }

  @Test
  public void findUrlsWhenMessageTextHasOnlyMaskedLinks() {
    // GIVEN
    String url = "http://link111.domen";
    TextFormatMetadata textFormatMetadata = new TextFormatMetadata();
    TextFormatMetadata.Link linkMetadata = new TextFormatMetadata.Link();
    linkMetadata.setOffset(0);
    linkMetadata.setLength(4);
    linkMetadata.setUrl(url);
    textFormatMetadata.setLink(List.of(linkMetadata));
    String textAsMask = "mask";
    Reply source = createForward(textAsMask, textFormatMetadata);
    UrlInStringFinder urlInStringFinder = Mockito.mock(UrlInStringFinder.class);
    UrlFinderInReply urlFinderInForward = new UrlFinderInReply(urlInStringFinder);
    // WHEN

    LinksInMessage urls = urlFinderInForward.findUrls(source);

    // THEN
    assertFalse(urls.getLinks().isEmpty());
    assertEquals(url, urls.getLinks().get(0).getLink());
    assertEquals(textAsMask, urls.getLinks().get(0).getMask());
    assertTrue(urls.getLinks().get(0).isMasked());
  }

  @Test
  public void findUrlsWhenMessageTextHasBothTypeLinks() {
    // GIVEN
    String urlForMask = "http://link111.domen";
    TextFormatMetadata textFormatMetadata = new TextFormatMetadata();
    TextFormatMetadata.Link linkMetadata = new TextFormatMetadata.Link();
    linkMetadata.setOffset(0);
    linkMetadata.setLength(4);
    linkMetadata.setUrl(urlForMask);
    textFormatMetadata.setLink(List.of(linkMetadata));
    String text = "mask https://someurl";
    Reply source = createForward(text, textFormatMetadata);
    UrlInStringFinder urlInStringFinder = Mockito.mock(UrlInStringFinder.class);
    UrlFinderInReply urlFinderInForward = new UrlFinderInReply(urlInStringFinder);
    when(urlInStringFinder.findUrls(text))
        .thenReturn(List.of(Link.of("https://someurl", "https://someurl", false)));
    // WHEN

    LinksInMessage urls = urlFinderInForward.findUrls(source);

    // THEN
    assertFalse(urls.getLinks().isEmpty());
    System.out.println(urls);
    assertEquals(2, urls.getLinks().size());
    assertTrue(urls.getLinks().stream().filter(Link::isMasked).findFirst().isPresent());
    assertTrue(
        urls.getLinks().stream().filter(Predicate.not(Link::isMasked)).findFirst().isPresent());
  }

  private Reply createForward(String text, TextFormatMetadata textFormatMetadata) {
    Reply source = new Reply();
    Reply.Data payload = new Reply.Data();
    Reply.ReplyMessage message = new Reply.ReplyMessage();
    message.setText(text);
    payload.setMessage(message);
    message.setFormat(textFormatMetadata);
    source.setPayload(payload);
    return source;
  }
}

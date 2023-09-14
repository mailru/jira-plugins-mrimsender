/* (C)2023 */
package ru.mail.jira.plugins.myteam.component.url;

import static java.util.stream.Collectors.toSet;
import static org.junit.Assert.*;

import java.util.List;
import java.util.Set;
import org.junit.Test;
import ru.mail.jira.plugins.myteam.component.url.dto.Link;

@SuppressWarnings("NullAway")
public class UrlInStringFinderTest {

  @Test
  public void findUrlsWhenTextHasNotLinks() {
    // GIVEN
    String text = "text";
    UrlInStringFinder urlInStringFinder = new UrlInStringFinder();

    // WHEN
    List<Link> urls = urlInStringFinder.findUrls(text);

    // THEN
    assertTrue(urls.isEmpty());
  }

  @Test
  public void findUrlsWhenTextNull() {
    // GIVEN
    String text = null;
    UrlInStringFinder urlInStringFinder = new UrlInStringFinder();

    // WHEN
    List<Link> urls = urlInStringFinder.findUrls(text);

    // THEN
    assertTrue(urls.isEmpty());
  }

  @Test
  public void findUrlsWhenHasSimpleLink() {
    // GIVEN
    String text = "https://vk.com";
    UrlInStringFinder urlInStringFinder = new UrlInStringFinder();

    // WHEN
    List<Link> urls = urlInStringFinder.findUrls(text);

    // THEN
    assertFalse(urls.isEmpty());
    assertEquals(text, urls.get(0).getLink());
    assertEquals(text, urls.get(0).getMask());
    assertFalse(urls.get(0).isMasked());
  }

  @Test
  public void findUrlsWhenHasComplexLink() {
    // GIVEN
    String text =
        "https://some.com/some1/some2 https://some.com/some1/some2/some3/some-4/some-5-/ \n https://some.com/some1/some2/some3/some-4/some-6-/";
    UrlInStringFinder urlInStringFinder = new UrlInStringFinder();

    // WHEN
    List<Link> urls = urlInStringFinder.findUrls(text);

    // THEN
    assertEquals(3, urls.size());
    assertTrue(urls.stream().noneMatch(Link::isMasked));
    assertEquals(
        Set.of(
            "https://some.com/some1/some2",
            "https://some.com/some1/some2/some3/some-4/some-5-/",
            "https://some.com/some1/some2/some3/some-4/some-6-/"),
        urls.stream().map(Link::getLink).collect(toSet()));
  }

  @Test
  public void findUrlsWhenHasComplexLinkInComplexString() {
    // GIVEN
    String text =
        "Text111 _!@)#_+!@+ Text https://some.com/some1/some2 \n\n\n\n\n\n Text _zc_D3123 Text https://some.com/some1/some2/some3/some-4/some-5-/ \n\n\n\n 22222 https://some.com/some1/some2/some3/some-4/some-6-/";
    UrlInStringFinder urlInStringFinder = new UrlInStringFinder();

    // WHEN
    List<Link> urls = urlInStringFinder.findUrls(text);

    // THEN
    assertEquals(3, urls.size());
    assertTrue(urls.stream().noneMatch(Link::isMasked));
    assertEquals(
        Set.of(
            "https://some.com/some1/some2",
            "https://some.com/some1/some2/some3/some-4/some-5-/",
            "https://some.com/some1/some2/some3/some-4/some-6-/"),
        urls.stream().map(Link::getLink).collect(toSet()));
  }
}

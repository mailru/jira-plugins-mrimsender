/* (C)2023 */
package ru.mail.jira.plugins.myteam.component;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class DiffFieldChatMessageGeneratorTest {

  @SuppressWarnings("NullAway")
  private static DiffFieldChatMessageGenerator diffFieldChatMessageGenerator;

  @BeforeAll
  static void setUp() {
    diffFieldChatMessageGenerator = new DiffFieldChatMessageGenerator();
  }

  @Test
  void markOldValue() {
    // GIVEN
    String oldValue = "oldValue";

    // WHEN
    String result = diffFieldChatMessageGenerator.markOldValue(oldValue);

    // THEN
    assertEquals("~oldValue~", result);
  }

  @Test
  void markOldValueWhenStringIsEmpty() {
    // GIVEN
    String oldValue = "";

    // WHEN
    String result = diffFieldChatMessageGenerator.markOldValue(oldValue);

    // THEN
    assertEquals("", result);
  }

  @Test
  void markOldValueWhenStringIsBlank() {
    // GIVEN
    String oldValue = "                  ";

    // WHEN
    String result = diffFieldChatMessageGenerator.markOldValue(oldValue);

    // THEN
    assertEquals("", result);
  }

  @Test
  void markOldValueWhenStringIsBlankWithNewLine() {
    // GIVEN
    String oldValue = "        \n          ";

    // WHEN
    String result = diffFieldChatMessageGenerator.markOldValue(oldValue);

    // THEN
    assertEquals("", result);
  }

  @Test
  void markNewValue() {
    // GIVEN
    String newValue = "newValue";

    // WHEN
    String result = diffFieldChatMessageGenerator.markNewValue(newValue);

    // THEN
    assertEquals("*newValue*", result);
  }

  @Test
  void markNewValueWhenStringIsEmpty() {
    // GIVEN
    String newValue = "";

    // WHEN
    String result = diffFieldChatMessageGenerator.markNewValue(newValue);

    // THEN
    assertEquals("", result);
  }

  @Test
  void markNewValueWhenStringIsBlank() {
    // GIVEN
    String newValue = "                     ";

    // WHEN
    String result = diffFieldChatMessageGenerator.markNewValue(newValue);

    // THEN
    assertEquals("", result);
  }

  @Test
  void markNewValueWhenStringIsBlankWithNewLine() {
    // GIVEN
    String newValue = "           \n          ";

    // WHEN
    String result = diffFieldChatMessageGenerator.markNewValue(newValue);

    // THEN
    assertEquals("", result);
  }

  @Test
  void buildDiffString() {
    // GIVEN
    String oldValue = "old value";
    String newValue = "new value";

    // WHEN
    String diff = diffFieldChatMessageGenerator.buildDiffString(oldValue, newValue);

    // THEN
    assertEquals("~old~ *new*  value ", diff);
  }

  @Test
  void buildDiffStringWhenBothStringAreEmpty() {
    // GIVEN
    String oldValue = "";
    String newValue = "";

    // WHEN
    String diff = diffFieldChatMessageGenerator.buildDiffString(oldValue, newValue);

    // THEN
    assertEquals("", diff);
  }

  @Test
  void buildDiffStringWhenBothStringAreBlanksWithSameLength() {
    // GIVEN
    String blank = "             ";
    String oldValue = blank;
    String newValue = blank;

    // WHEN
    String diff = diffFieldChatMessageGenerator.buildDiffString(oldValue, newValue);

    // THEN
    assertEquals(" ", diff);
  }

  @Test
  void buildDiffStringWhenBothStringAreBlanksWithDiffLength() {
    // GIVEN
    String oldValue = "          ";
    String newValue = "             ";

    // WHEN
    String diff = diffFieldChatMessageGenerator.buildDiffString(oldValue, newValue);

    // THEN
    assertEquals("~~ ** ", diff);
  }

  @Test
  void buildDiffStringWhenBothStringHasComplexLines() {
    // GIVEN
    String oldValue = "Text\n" + "text123\n\n\n\n";
    String newValue = "Text\n\n\n\nnewtext123";

    // WHEN
    String diff = diffFieldChatMessageGenerator.buildDiffString(oldValue, newValue);

    // THEN
    assertEquals("Text\n" + " ~text123~ \n" + "\n" + "\n" + " ~\n" + "~ *newtext123* ", diff);
  }

  @Test
  void buildDiffStringWhenBothStringAreFloatNumberWithSomeText() {
    // GIVEN
    String oldValue = "8.20\n" + "text123\n\n\n\n";
    String newValue = "8.21\n" + "text12hello465456\n\n\n\n";

    // WHEN
    String diff = diffFieldChatMessageGenerator.buildDiffString(oldValue, newValue);

    // THEN
    assertEquals("8. ~20~ *21* \n" + " ~text123~ *text12hello465456* \n\n\n\n ", diff);
  }

  @Test
  void buildDiffStringWhenOldValueEmptyString() {
    // GIVEN
    String oldValue = "";
    String newValue = "8.21\n" + "text12hello465456\n\n\n\n";

    // WHEN
    String diff = diffFieldChatMessageGenerator.buildDiffString(oldValue, newValue);

    // THEN
    assertEquals("*8.21\n" + "text12hello465456\n\n\n\n* ", diff);
  }

  @Test
  void buildDiffStringWhenNewValueEmptyString() {
    // GIVEN
    String oldValue = "8.21\n" + "text12hello465456\n\n\n\n";
    String newValue = "";

    // WHEN
    String diff = diffFieldChatMessageGenerator.buildDiffString(oldValue, newValue);

    // THEN
    assertEquals("~8.21\n" + "text12hello465456\n\n\n\n~ ", diff);
  }

  @Test
  void buildDiffStringWhenStringsHasJiraMarkdownAttachmentsNames() {
    // GIVEN
    String oldValue = "!image.jpg|thumbnail!";
    String newValue = "!new.jpg|thumbnail!";

    // WHEN
    String diff = diffFieldChatMessageGenerator.buildDiffString(oldValue, newValue);

    // THEN
    assertEquals("! ~image~ *new* .jpg|thumbnail! ", diff);
  }

  @Test
  void buildDiffStringWhenStringsHasManyMultilines() {
    // GIVEN
    String oldValue =
        "Какой-то текст\n\n\n"
            + "еще какой-то текст 8.20\n\n"
            + "текст еще идет !image.jpg|thumbnail!\n"
            + "все также идет usermail.domen\n\nhttp://some.site.domen\n\n\n"
            + "!image1.jpg|thumbnail!";

    String newValue =
        "Какой-то текст\n\n\n"
            + "еще какой-т текст 8.2234\n\n"
            + "текст еще идет \n"
            + "все также идет usermail.domen\n\nhttps://some.site.domen\n\n\n"
            + "!image2.jpg|thumbnail!";

    // WHEN
    String diff = diffFieldChatMessageGenerator.buildDiffString(oldValue, newValue);

    // THEN
    assertEquals(
        "Какой-то текст\n\n\n"
            + "еще какой- ~то~ *т*  текст 8. ~20~ *2234* \n\n"
            + "текст еще идет ~ !image.jpg|thumbnail!\n~ * \n"
            + "* все также идет usermail.domen\n\n"
            + " ~http~ *https* ://some.site.domen\n\n\n"
            + "! ~image1~ *image2* .jpg|thumbnail! ",
        diff);
  }
}

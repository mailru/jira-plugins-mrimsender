/* (C)2021 */
package ru.mail.jira.plugins.myteam.repository.myteam.dto.parts;

import org.jetbrains.annotations.Nullable;

public enum CommentaryParts {
  File,
  Forward,
  Mention,
  Part;

  @Nullable
  public static CommentaryParts fromPartClass(final Class clazz) {
    if (File.name().equalsIgnoreCase(clazz.getSimpleName())) {
      return File;
    } else if (Forward.name().equalsIgnoreCase(clazz.getSimpleName())) {
      return Forward;
    } else if (Mention.name().equalsIgnoreCase(clazz.getSimpleName())) {
      return Mention;
    } else if (Part.name().equalsIgnoreCase(clazz.getSimpleName())) {
      return Part;
    } else {
      return null;
    }
  }
}

/* (C)2023 */
package ru.mail.jira.plugins.myteam.component.url.dto;

import java.util.List;
import lombok.Value;

@Value(staticConstructor = "of")
public class LinksInMessage {
  List<Link> links;
}

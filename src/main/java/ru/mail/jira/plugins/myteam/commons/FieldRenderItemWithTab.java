/* (C)2022 */
package ru.mail.jira.plugins.myteam.commons;

import com.atlassian.jira.issue.fields.rest.json.beans.FieldTab;
import com.atlassian.jira.issue.fields.screen.FieldScreenRenderLayoutItem;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class FieldRenderItemWithTab {

  private FieldScreenRenderLayoutItem fieldScreenRenderLayoutItem;
  private FieldTab fieldTab;
}

/* (C)2022 */
package ru.mail.jira.plugins.myteam.commons;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import ru.mail.jira.plugins.myteam.service.dto.FieldDto;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@XmlRootElement
public class FieldCollection {
  @XmlElement private List<FieldDto> all;
  @XmlElement private List<FieldDto> other;
  @XmlElement private List<FieldDto> users;
  @XmlElement private List<FieldDto> dates;

  public void setOther(List<FieldDto> other) {
    this.other = other;
    updateAll();
  }

  private void updateAll() {
    ArrayList<FieldDto> all = new ArrayList<>();
    if (other != null) all.addAll(other);
    if (users != null) all.addAll(users);
    if (dates != null) all.addAll(dates);
    this.all = all;
  }

  public void setUsers(List<FieldDto> users) {
    this.users = users;
    updateAll();
  }

  public void setDates(List<FieldDto> dates) {
    this.dates = dates;
    updateAll();
  }
}

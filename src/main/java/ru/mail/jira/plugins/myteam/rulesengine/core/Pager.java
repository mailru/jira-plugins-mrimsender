/* (C)2021 */
package ru.mail.jira.plugins.myteam.rulesengine.core;

import lombok.Getter;
import lombok.Setter;

@Getter
public class Pager {
  private int page = 0;
  @Setter private int total;
  private final int perPage;

  public Pager(int total, int perPage) {
    this.total = total;
    this.perPage = perPage;
  }

  public void nextPage() {
    page++;
    //    if (total / perPage < page) return;
  }

  public void prevPage() {
    if (page == 0) return;
    page--;
  }

  public String getPagerStatus() {
    if (total < perPage) {
      return "";
    }
    return String.format(
        "%s-%s/%s", getPage() * getPerPage() + 1, (getPage() + 1) * getPerPage(), getTotal());
  }

  public boolean hasNext() {
    return (total / perPage > page + 1);
  }

  public boolean hasPrev() {
    return page != 0;
  }

  public boolean hasPages() {
    return total / perPage > 0;
  }
}

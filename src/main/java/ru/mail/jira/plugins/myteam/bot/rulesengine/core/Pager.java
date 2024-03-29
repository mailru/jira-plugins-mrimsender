/* (C)2021 */
package ru.mail.jira.plugins.myteam.bot.rulesengine.core;

import lombok.Getter;
import lombok.Setter;

@Getter
public class Pager {
  @Setter private int page = 0;
  @Setter private int total;
  private final int pageSize;

  public Pager(int total, int perPage) {
    this.total = total;
    this.pageSize = perPage;
  }

  public void nextPage() {
    if (total / pageSize < page) return;
    page++;
  }

  public void prevPage() {
    if (page == 0) return;
    page--;
  }

  public String getPagerStatus() {
    if (total < pageSize) {
      return "";
    }
    return String.format(
        "%s-%s/%s",
        getPage() * this.getPageSize() + 1, (getPage() + 1) * this.getPageSize(), getTotal());
  }

  public boolean hasNext() {
    return Math.ceil((float) total / pageSize) - page > 1;
  }

  public boolean hasPrev() {
    return page != 0;
  }

  public boolean hasPages() {
    return total / pageSize > 0;
  }
}

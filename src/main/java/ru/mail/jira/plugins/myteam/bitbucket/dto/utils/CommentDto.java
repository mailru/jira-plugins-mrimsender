/* (C)2021 */
package ru.mail.jira.plugins.myteam.bitbucket.dto.utils;

import java.util.List;
import lombok.*;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@ToString
public class CommentDto {
  private long id;
  private long version;
  private String text;
  private UserDto author;
  private long createdDate;
  private long updatedDate;
  private List<CommentDto> comments;

  // TODO properties, tasks, permittedOperations
}

/*
EXAMPLE:
"comment":{
    "properties":{
      "repositoryId":84
    },
    "id":42,
    "version":0,
    "text":"This is a great line of code!",
    "author":{
      "name":"admin",
      "emailAddress":"admin@example.com",
      "id":1,
      "displayName":"Administrator",
      "active":true,
      "slug":"admin",
      "type":"NORMAL"
    },
    "createdDate":1505778786337,
    "updatedDate":1505778786337,
    "comments":[

    ],
    "tasks":[

    ],
    "permittedOperations":{
      "editable":true,
      "deletable":true
    }
  },
 */

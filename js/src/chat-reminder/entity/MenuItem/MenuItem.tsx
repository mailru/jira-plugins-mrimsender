import {useReminderDialog} from "../../widgets";
import {I18n} from "@atlassian/wrm-react-i18n";
import React from "react";

const MenuItem = () => {
    const showDialog = useReminderDialog()
    return (
        // eslint-disable-next-line jsx-a11y/click-events-have-key-events, jsx-a11y/no-static-element-interactions
        <a href="#" className='aui-list-item-link js-issueaction reminder-menu-item' onClick={showDialog}>
            {I18n.getText('ru.mail.jira.plugins.myteam.reminder.title')}
        </a>
    )
}

export default MenuItem
import { observer } from 'mobx-react';
import React from 'react';
import styled from '@emotion/styled';
import { I18n } from '@atlassian/wrm-react-i18n';
import AvatarGroup from '@atlaskit/avatar-group';

import MyteamImage from '../../assets/myteam.png';
import { ChatInfoType } from '../stores/types';

type ChatInfoProps = {
  chatInfo: ChatInfoType;
};

export const StyledContainer = styled.div`
  align-items: center;
  display: flex;
`;

const StyledLabel = styled.span`
  color: #6b778c;
  flex: 0 0 140px;
`;

const Logo = styled.img`
  padding-right: 5px;
  vertical-align: middle;
`;

export const ChatInfo = observer((props: ChatInfoProps) => {
  const { chatInfo } = props;
  return (
    <div>
      <a href={chatInfo.link} target="_blank" rel="noreferrer">
        <Logo height={16} src={MyteamImage} alt="Myteam logo" />
        {chatInfo.name}
      </a>
      <StyledContainer>
        <StyledLabel>
          {I18n.getText(
            'ru.mail.jira.plugins.myteam.createChat.panel.members.preview',
          )}
        </StyledLabel>
        <AvatarGroup
          appearance="stack"
          data={chatInfo.members.map((member: any) => ({
            name: member.name,
            id: member.id,
            src: member.avatarUrl,
          }))}
        />
      </StyledContainer>
    </div>
  );
});

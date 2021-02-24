import {observer} from 'mobx-react';
import React from 'react';
import styled from '@emotion/styled';
import {ChatInfoType} from '../stores/LoadingService';
import {I18n} from '@atlassian/wrm-react-i18n';
import AvatarGroup from '@atlaskit/avatar-group';

// eslint-disable-next-line @typescript-eslint/no-var-requires
const MyteamImage = require('../../assets/myteam.png');

type ChatInfoProps = {
    chatInfo: ChatInfoType;
};

export const StyledContainer = styled.div`
  display: flex;
  justify-content: space-between;
  line-height: 1.5;
`;

export const StyledDt = styled.dt`
  color: #6b778c;
  width: 140px;
  display: table-cell;
  vertical-align:middle;
`;

export const StyledDd = styled.dd`
  display: table-cell;
`;

const StyledValue = styled.span`
  text-align: left;
  display:table;
`;

const StyledSpan = styled.span`
    display:table-cell;
    vertical-align:middle;
    padding-left:5px;
`;

export const ChatInfo = observer((props: ChatInfoProps) => {
    const {chatInfo} = props;
    const avatarGroupData = chatInfo.members.map(function (member) {
        return {
            name: member.displayName,
            key: member.id,
            avatarUrl: member.avatarUrl,
        }
    });
    return (
        <div>
            <StyledContainer>
                <a href={chatInfo.link} target="_blank" rel="noreferrer">
                    <StyledValue>
                        <img height={24} src={MyteamImage.default} alt="Myteam logo"/>
                        <StyledSpan>{chatInfo.name}</StyledSpan>
                    </StyledValue>
                </a>

            </StyledContainer>
            <StyledContainer>
                <dl>
                    <StyledDt>{I18n.getText('ru.mail.jira.plugins.myteam.createChat.panel.members.preview')}</StyledDt>
                    <StyledDd>
                        <AvatarGroup appearance="stack" data={avatarGroupData}
                                     overrides={{
                                         Avatar: {
                                             render: (Component, props, index) => {
                                                 return <Component {...props} src={avatarGroupData[index].avatarUrl}/>;
                                             },
                                         },
                                     }}
                        />
                    </StyledDd>
                </dl>
            </StyledContainer>
        </div>
    );
});

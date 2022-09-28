import React, { ReactElement } from 'react';
import {
  ButtonItem,
  NavigationHeader,
  Section,
  SideNavigation,
} from '@atlaskit/side-navigation';
import styled from 'styled-components';
import { I18n } from '@atlassian/wrm-react-i18n';
import MyteamImage from '../../assets/myteam.png';
import UserFilterSubscriptions from './UserFilterSubscriptions';

const FilterSubscriptions = styled.div`
  display: flex;
`;

const LeftMenu = styled.div`
  width: 240px;
  background-color: #f4f5f7;
`;

const Content = styled.div`
  background-color: #fff;
  width: 100%;
  padding: 20px;
`;

const Header = styled.div`
  display: flex;
  align-items: center;

  h4 {
    margin: 0;
  }
`;

const Logo = styled.img`
  padding-right: 5px;
  vertical-align: middle;
`;

function ManageFilterSubscriptions(): ReactElement {
  const [view, setView] = React.useState<string>('my');

  return (
    <FilterSubscriptions>
      <LeftMenu>
        <SideNavigation label="project" testId="side-navigation">
          <NavigationHeader>
            <Header>
              <Logo height={24} src={MyteamImage} alt="Myteam logo" />
              <h4>
                {I18n.getText(
                  'ru.mail.jira.plugins.myteam.subscriptions.page.title',
                )}
              </h4>
            </Header>
          </NavigationHeader>
          <Section>
            <ButtonItem
              isSelected={view === 'my'}
              onClick={() => setView('my')}
            >
              My Subscriptions
            </ButtonItem>
            <ButtonItem
              isSelected={view === 'search'}
              onClick={() => setView('search')}
            >
              Search filter
            </ButtonItem>
          </Section>
        </SideNavigation>
      </LeftMenu>
      <Content>
        {view === 'my' ? <UserFilterSubscriptions /> : <div>Hello World</div>}
      </Content>
    </FilterSubscriptions>
  );
}

export default ManageFilterSubscriptions;

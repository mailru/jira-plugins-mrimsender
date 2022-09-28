import React, {ReactElement} from 'react';
import styled from 'styled-components';
import { I18n } from '@atlassian/wrm-react-i18n';
import MyteamImage from '../../assets/myteam.png';
import contextPath from "wrm/context-path";
import DropdownMenu, {DropdownItem, DropdownItemGroup} from "@atlaskit/dropdown-menu";
import Button from "@atlaskit/button";
import MoreIcon from "@atlaskit/icon/glyph/more";
import DynamicTable from "@atlaskit/dynamic-table";
import {useGetSubscriptions} from "../../shared/hooks";
import {FilterSubscription} from "../../shared/types";

const Page = styled.div`
  background-color: #fff;
  padding: 20px;
`;

const Header = styled.div`
  display: flex;
  align-items: center;
    justify-content: space-between;
`

const Left = styled.div`
  display: flex;
  align-items: center;

  h1 {
    margin: 0;
  }
`;

const Logo = styled.img`
  padding-right: 5px;
  vertical-align: middle;
`;

const PageDescription = styled.div`
  padding-top: 10px;
  padding-bottom: 10px;
`;
const Cell = styled.div`
  margin-top: 5px;
  margin-bottom: 5px;
`;

const tableHead = {
  cells: [
    {
      content: I18n.getText('report.chart.filter'),
      key: 'filter',
      isSortable: false,
    },
    {
      content: I18n.getText('subscriptions.subscribed'),
      key: 'subscribed',
      isSortable: false,
    },
    {
      content: I18n.getText('filtersubscription.field.schedule'),
      key: 'schedule',
      isSortable: false,
    },
    {
      content: I18n.getText('admin.schedulerdetails.last.run'),
      key: 'lastRun',
      isSortable: false,
    },
    {
      content: I18n.getText('admin.schedulerdetails.next.run'),
      key: 'nextRun',
      isSortable: false,
    },
    {
      content: I18n.getText('common.words.actions'),
      key: 'actions',
      isSortable: false,
    },
  ],
};

const buildRows = (subscriptions?: FilterSubscription[]) => {
  return subscriptions?.map((subscription) => ({
    cells: [
      {
        key: subscription.filter.id,
        content: (
          <Cell>
            <a
              href={`${contextPath()}/issues/?filter=${
                subscription.filter.id
              }`}
              target="_blank"
              rel="noreferrer"
            >
              {subscription.filter.name}
            </a>
          </Cell>
        ),
      },
      {
        key: subscription.groupName,
        content: (
          <Cell>
            {subscription.groupName === undefined
              ? subscription.user.displayName
              : subscription.groupName}
          </Cell>
        ),
      },
      {
        key: subscription.cronExpressionDescription,
        content: <Cell>{subscription.cronExpressionDescription}</Cell>,
      },
      {
        key: subscription.lastRun,
        content: <Cell>{subscription.lastRun}</Cell>,
      },
      {
        key: subscription.nextRun,
        content: <Cell>{subscription.nextRun}</Cell>,
      },
      {
        content: (
          <Cell>
            <DropdownMenu
              trigger={({ triggerRef, ...props }) => (
                <Button
                  {...props}
                  appearance="subtle"
                  iconBefore={<MoreIcon label="more" />}
                  ref={triggerRef}
                />
              )}
            >
              <DropdownItemGroup>
                <DropdownItem>
                  {I18n.getText('common.forms.run.now')}
                </DropdownItem>
                <DropdownItem>
                  {I18n.getText('common.words.edit')}
                </DropdownItem>
                <DropdownItem>
                  {I18n.getText('common.words.delete')}
                </DropdownItem>
              </DropdownItemGroup>
            </DropdownMenu>
          </Cell>
        ),
      },
    ],
  }));
};

function ManageFilterSubscriptions(): ReactElement {
  const subscriptions = useGetSubscriptions();

  return (
    <Page>
      <Header>
        <Left>
          <Logo height={48} src={MyteamImage} alt="Myteam logo" />
          <h1>
            {I18n.getText(
              'ru.mail.jira.plugins.myteam.subscriptions.page.title',
            )}
          </h1>
        </Left>
        <Button>{I18n.getText('subscriptions.add')}</Button>
      </Header>
      <PageDescription>
        {I18n.getText(
          'ru.mail.jira.plugins.myteam.subscriptions.page.my.description',
        )}
      </PageDescription>
      <DynamicTable
        head={tableHead}
        rows={buildRows(subscriptions.data)}
        rowsPerPage={20}
        defaultPage={1}
        isLoading={subscriptions.isLoading}
        emptyView={
          <h2>
            {I18n.getText(
              'ru.mail.jira.plugins.myteam.subscriptions.page.my.empty',
            )}
          </h2>
        }
        loadingSpinnerSize="large"
      />
    </Page>
  );
}

export default ManageFilterSubscriptions;

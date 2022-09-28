import React, { ReactElement, useLayoutEffect, useState } from 'react';
import DynamicTable from '@atlaskit/dynamic-table';
import { I18n } from '@atlassian/wrm-react-i18n';
import styled from 'styled-components';
import contextPath from 'wrm/context-path';
import DropdownMenu, {
  DropdownItem,
  DropdownItemGroup,
} from '@atlaskit/dropdown-menu';
import Button from '@atlaskit/button';
import MoreIcon from '@atlaskit/icon/glyph/more';
import { FilterSubscription, LoadableDataState } from '../../shared/types';
import { loadUserSubscriptions } from '../../shared/api/SubscriptionsApiClient';

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

function UserFilterSubscriptions(): ReactElement {
  const [subscriptions, setSubscriptions] = useState<
    LoadableDataState<Array<FilterSubscription>>
  >({
    isLoading: false,
  });

  useLayoutEffect(() => {
    setSubscriptions({ isLoading: true });
    loadUserSubscriptions()
      .then((response) =>
        setSubscriptions({ data: response.data, isLoading: false }),
      )
      .catch((e) => {
        console.error(e);
        setSubscriptions({ isLoading: false, error: JSON.stringify(e) });
      });
  }, []);

  const buildRows = () => {
    if (subscriptions.data === undefined) return [];
    return subscriptions.data.map((subscription) => ({
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

  return (
    <div>
      <h1>
        {I18n.getText(
          'ru.mail.jira.plugins.myteam.subscriptions.page.my.header',
        )}
      </h1>
      <PageDescription>
        {I18n.getText(
          'ru.mail.jira.plugins.myteam.subscriptions.page.my.description',
        )}
      </PageDescription>
      <DynamicTable
        head={tableHead}
        rows={buildRows()}
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
    </div>
  );
}

export default UserFilterSubscriptions;

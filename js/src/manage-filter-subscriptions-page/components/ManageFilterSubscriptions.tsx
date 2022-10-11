/* eslint-disable react/jsx-props-no-spreading */
import React, { ReactElement, useState } from 'react';
import styled from 'styled-components';
import { I18n } from '@atlassian/wrm-react-i18n';
import contextPath from 'wrm/context-path';
import DropdownMenu, {
  DropdownItem,
  DropdownItemGroup,
} from '@atlaskit/dropdown-menu';
import Button from '@atlaskit/button';
import MoreIcon from '@atlaskit/icon/glyph/more';
import DynamicTable from '@atlaskit/dynamic-table';
import MyteamImage from '../../assets/myteam.png';
import {
  useGetSubscriptions,
  useSubscriptionDelete,
  useSubscriptionMutation,
} from '../../shared/hooks';
import { FilterSubscription } from '../../shared/types';
import ConfirmationDialog from '../../shared/components/dialogs/ConfirmationDialog';
import CreateFilterSubscriptionDialog from './CreateFilterSubscriptionDialog';
import EditFilterSubscriptionDialog from "./EditFilterSubscriptionDialog";

const Page = styled.div`
  background-color: #fff;
  padding: 20px;
`;

const Header = styled.div`
  display: flex;
  align-items: center;
  justify-content: space-between;
`;

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

type BuildRowsProps = {
  subscriptions?: FilterSubscription[];
  selectSubscription: (subscription: FilterSubscription) => void;
  openEditDialog: (open: boolean) => void;
  openDeleteDialog: (open: boolean) => void;
};

const buildRows = ({
  subscriptions,
  selectSubscription,
  openEditDialog,
  openDeleteDialog,
}: BuildRowsProps) => {
  return subscriptions?.map((subscription) => ({
    cells: [
      {
        key: subscription.filter?.id,
        content: (
          <Cell>
            <a
              href={`${contextPath()}/issues/?filter=${
                subscription.filter?.id
              }`}
              target="_blank"
              rel="noreferrer"
            >
              {subscription.filter?.name}
            </a>
          </Cell>
        ),
      },
      {
        key: subscription.recipientsType,
        content: (
          <Cell>
            {subscription.recipientsType === 'USER' &&
              subscription.users?.map((user) => (
                <div key={user.userKey}>{user.displayName}</div>
              ))}
            {subscription.recipientsType === 'GROUP' &&
              subscription.groups?.map((group) => (
                <div key={group}>{group}</div>
              ))}
            {subscription.recipientsType === 'CHAT' &&
              subscription.chats?.map((chat) => <div key={chat}>{chat}</div>)}
          </Cell>
        ),
      },
      {
        key: subscription.scheduleDescription,
        content: <Cell>{subscription.scheduleMode === 'advanced' ? subscription.advanced : subscription.scheduleDescription}</Cell>,
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
                <DropdownItem
                  onClick={() => {
                    selectSubscription(subscription);
                    openEditDialog(true);
                  }}
                >
                  {I18n.getText('common.words.edit')}
                </DropdownItem>
                <DropdownItem
                  onClick={() => {
                    selectSubscription(subscription);
                    openDeleteDialog(true);
                  }}
                >
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
  const [openCreateSubscriptionDialog, setOpenCreateSubscriptionDialog] =
    useState<boolean>(false);
  const [openEditSubscriptionDialog, setOpenEditSubscriptionDialog] =
    useState<boolean>(false);
  const [openDeleteSubscriptionDialog, setOpenDeleteSubscriptionDialog] =
    useState<boolean>(false);
  const [selectedSubscription, setSelectedSubscription] =
    useState<FilterSubscription>();

  const subscriptions = useGetSubscriptions();
  const deleteSubscription = useSubscriptionDelete();
  const subscriptionMutation = useSubscriptionMutation();

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
        <Button onClick={() => setOpenCreateSubscriptionDialog(true)}>
          {I18n.getText('subscriptions.add')}
        </Button>
      </Header>
      <PageDescription>
        {I18n.getText(
          'ru.mail.jira.plugins.myteam.subscriptions.page.description',
        )}
      </PageDescription>
      <DynamicTable
        head={tableHead}
        rows={buildRows({
          subscriptions: subscriptions.data,
          selectSubscription: setSelectedSubscription,
          openEditDialog: setOpenEditSubscriptionDialog,
          openDeleteDialog: setOpenDeleteSubscriptionDialog,
        })}
        rowsPerPage={20}
        defaultPage={1}
        isLoading={subscriptions.isLoading}
        emptyView={
          <h2>
            {I18n.getText(
              'ru.mail.jira.plugins.myteam.subscriptions.page.empty',
            )}
          </h2>
        }
        loadingSpinnerSize="large"
      />
      <CreateFilterSubscriptionDialog
        isOpen={openCreateSubscriptionDialog}
        onClose={() => {
          setOpenCreateSubscriptionDialog(false);
          subscriptionMutation.reset();
        }}
        onSaveSuccess={(subscription) => {
          subscriptionMutation.mutate(subscription, {
            onSuccess: () => {
              setOpenCreateSubscriptionDialog(false);
              subscriptions.refetch();
            },
          });
        }}
        creationError={subscriptionMutation.error?.response?.data}
      />
      {selectedSubscription && selectedSubscription.id &&
        <EditFilterSubscriptionDialog
          isOpen={openEditSubscriptionDialog}
          currentValue={selectedSubscription}
          onClose={() => {
            setOpenEditSubscriptionDialog(false);
            subscriptionMutation.reset();
          }}
          onSaveSuccess={(subscription) => {
            subscriptionMutation.mutate(subscription, {
              onSuccess: () => {
                subscriptions.refetch();
                setOpenEditSubscriptionDialog(false);
              },
            });
          }}
          editingError={subscriptionMutation.error?.response?.data}
        />
      }
      <ConfirmationDialog
        isOpen={openDeleteSubscriptionDialog}
        title={I18n.getText(
          'ru.mail.jira.plugins.myteam.subscriptions.page.subscription.delete',
        )}
        body={I18n.getText(
          'ru.mail.jira.plugins.myteam.subscriptions.page.subscription.delete.description',
        )}
        onOk={() => {
          if (selectedSubscription && selectedSubscription.id) {
            deleteSubscription.mutate(selectedSubscription.id, {
              onSuccess: () => {
                setOpenDeleteSubscriptionDialog(false);
                subscriptions.refetch();
              },
            });
          }
        }}
        onCancel={() => setOpenDeleteSubscriptionDialog(false)}
      />
    </Page>
  );
}

export default ManageFilterSubscriptions;

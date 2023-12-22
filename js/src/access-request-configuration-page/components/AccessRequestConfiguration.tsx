import React, { ReactElement, useState } from 'react'
import styled from 'styled-components'
import { I18n } from '@atlassian/wrm-react-i18n'
import PersonIcon from '@atlaskit/icon/glyph/person'
import PeopleGroupIcon from '@atlaskit/icon/glyph/people-group'
import PeopleIcon from '@atlaskit/icon/glyph/people'
import AppAccessIcon from '@atlaskit/icon/glyph/app-access'
import UnlockFilledIcon from '@atlaskit/icon/glyph/unlock-filled'
import Button from '@atlaskit/button'
import Spinner from '@atlaskit/spinner'
import CreateConfigurationDialog from './CreateConfigurationDialog'
import AccessRequestImage from '../../assets/access-request.png'
import {
  useAccessRequestConfigurationDelete,
  useAccessRequestConfigurationMutation,
  useGetAccessRequestConfiguration,
} from '../../shared/hooks'
import EditConfigurationDialog from './EditConfigurationDialog'
import ConfirmationDialog from '../../shared/components/dialogs/ConfirmationDialog'

type Props = {
  projectKey: string | null
}

const Container = styled.div`
  h2 {
    margin-bottom: 20px;
  }
`

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
`

const Right = styled.div`
  display: flex;
  align-items: center;

  button {
    margin-right: 5px;
  }
`

const EmptyPageContainer = styled.div`
  margin: 48px auto;
  text-align: center;
  width: 500px;

  h4 {
    margin-bottom: 16px;
  }

  p {
    margin-bottom: 24px;
  }
`

const PageDescription = styled.div`
  padding-block: 10px;
`

const Recipients = styled.div`
  margin-bottom: 20px;
`

const Recipient = styled.div`
  display: flex;
  align-items: center;
  padding: 2px;

  div:last-child {
    margin-left: 5px;
  }
`

const Notifications = styled.div`
  margin-bottom: 20px;
`

function AccessRequestConfiguration({ projectKey }: Props): ReactElement {
  if (projectKey == null)
    return (
      <EmptyPageContainer>
        <h2>
          {I18n.getText(
            'ru.mail.jira.plugins.myteam.accessRequest.configuration.page.error.project'
          )}
        </h2>
      </EmptyPageContainer>
    )

  const [isOpenCreateConfigurationDialog, setIsOpenCreateConfigurationDialog] =
    useState<boolean>(false)
  const [isOpenEditConfigurationDialog, setIsOpenEditConfigurationDialog] =
    useState<boolean>(false)
  const [isOpenDeleteConfigurationDialog, setIsOpenDeleteConfigurationDialog] =
    useState<boolean>(false)

  const configuration = useGetAccessRequestConfiguration(projectKey)
  const configurationMutation = useAccessRequestConfigurationMutation()
  const deleteConfiguration = useAccessRequestConfigurationDelete()

  return (
    <Container>
      <Header>
        <Left>
          <h1>
            {I18n.getText(
              'ru.mail.jira.plugins.myteam.accessRequest.configuration.page.title'
            )}
          </h1>
        </Left>
        {configuration.data && (
          <Right>
            <Button onClick={() => setIsOpenEditConfigurationDialog(true)}>
              {I18n.getText('common.words.edit')}
            </Button>
            <Button onClick={() => setIsOpenDeleteConfigurationDialog(true)}>
              {I18n.getText('common.words.delete')}
            </Button>
          </Right>
        )}
      </Header>
      {configuration.isLoading ? (
        <Spinner size="large" />
      ) : configuration.data ? (
        <div>
          <PageDescription>
            {I18n.getText(
              'ru.mail.jira.plugins.myteam.accessRequest.configuration.page.description'
            )}
          </PageDescription>
          <Recipients>
            <h2>
              {I18n.getText(
                'ru.mail.jira.plugins.myteam.accessRequest.configuration.page.table.participants'
              )}
            </h2>
            {configuration.data.users?.map(({ userKey, displayName }) => (
              <Recipient
                key={userKey}
                title={I18n.getText('common.words.user')}
              >
                <PersonIcon size="small" label="" />
                <div>{displayName}</div>
              </Recipient>
            ))}
            {configuration.data.groups?.map((group) => (
              <Recipient key={group} title={I18n.getText('common.words.group')}>
                <PeopleGroupIcon size="small" label="" />
                <div>{group}</div>
              </Recipient>
            ))}
            {configuration.data.projectRoles?.map(({ id, name }) => (
              <Recipient
                key={id}
                title={I18n.getText('admin.projects.project.roles')}
              >
                <AppAccessIcon size="small" label="" />
                <div>{name}</div>
              </Recipient>
            ))}
            {configuration.data.userFields?.map(({ id, name }) => (
              <Recipient
                key={id}
                title={I18n.getText(
                  'ru.mail.jira.plugins.myteam.accessRequest.configuration.page.dialog.field.userFields'
                )}
              >
                <PeopleIcon size="small" label="" />
                <div>{name}</div>
              </Recipient>
            ))}
              <h2>
                  {I18n.getText(
                      'ru.mail.jira.plugins.myteam.accessRequest.configuration.page.table.accessRequest'
                  )}
              </h2>
              {configuration.data.accessPermissionFields?.map(({ id, name }) => (
                  <Recipient
                      key={id}
                      title={I18n.getText(
                          'ru.mail.jira.plugins.myteam.accessRequest.configuration.page.dialog.field.userFields'
                      )}
                  >
                      <UnlockFilledIcon size="small" label="" />
                      <div>{name}</div>
                  </Recipient>
              ))}
          </Recipients>
          <Notifications>
            <h2>
              {I18n.getText(
                'ru.mail.jira.plugins.myteam.accessRequest.configuration.page.table.notifications'
              )}
            </h2>
            {configuration.data.sendEmail && (
              <div>
                {I18n.getText(
                  'ru.mail.jira.plugins.myteam.accessRequest.configuration.page.table.notifications.sendEmail'
                )}
              </div>
            )}
            {configuration.data.sendMessage && (
              <div>
                {I18n.getText(
                  'ru.mail.jira.plugins.myteam.accessRequest.configuration.page.table.notifications.sendMessage'
                )}
              </div>
            )}
          </Notifications>
        </div>
      ) : (
        <EmptyPageContainer>
          <img
            src={AccessRequestImage}
            alt=""
            width="500"
            height="250"
            loading="lazy"
          />
          <h2>
            {I18n.getText(
              'ru.mail.jira.plugins.myteam.accessRequest.configuration.page.empty.title'
            )}
          </h2>
          <p>
            {I18n.getText(
              'ru.mail.jira.plugins.myteam.accessRequest.configuration.page.empty.description'
            )}
          </p>
          <Button
            appearance="primary"
            onClick={() => setIsOpenCreateConfigurationDialog(true)}
          >
            {I18n.getText(
              'ru.mail.jira.plugins.myteam.accessRequest.configuration.page.button.configure'
            )}
          </Button>
        </EmptyPageContainer>
      )}
      <CreateConfigurationDialog
        projectKey={projectKey}
        isOpen={isOpenCreateConfigurationDialog}
        onClose={() => {
          setIsOpenCreateConfigurationDialog(false)
          configurationMutation.reset()
        }}
        onSaveSuccess={(config) =>
          configurationMutation.mutate(config, {
            onSuccess: () => {
              setIsOpenCreateConfigurationDialog(false)
              configuration.refetch()
            },
          })
        }
        creationError={configurationMutation.error?.response?.data}
      />
      {configuration.data?.id && (
        <EditConfigurationDialog
          projectKey={configuration.data.projectKey}
          isOpen={isOpenEditConfigurationDialog}
          currentValue={configuration.data}
          onClose={() => {
            setIsOpenEditConfigurationDialog(false)
            configurationMutation.reset()
          }}
          onSaveSuccess={(config) => {
            configurationMutation.mutate(config, {
              onSuccess: () => {
                configuration.refetch()
                setIsOpenEditConfigurationDialog(false)
              },
            })
          }}
          editingError={configurationMutation.error?.response?.data}
        />
      )}
      <ConfirmationDialog
        isOpen={isOpenDeleteConfigurationDialog}
        title={I18n.getText(
          'ru.mail.jira.plugins.myteam.accessRequest.configuration.page.dialog.delete.title'
        )}
        body={I18n.getText(
          'ru.mail.jira.plugins.myteam.accessRequest.configuration.page.dialog.delete.description'
        )}
        onOk={() => {
          if (configuration.data?.id) {
            deleteConfiguration.mutate(
              {
                projectKey: configuration.data.projectKey,
                id: configuration.data.id,
              },
              {
                onSuccess: () => {
                  setIsOpenDeleteConfigurationDialog(false)
                  configuration.refetch()
                },
              }
            )
          }
        }}
        onCancel={() => setIsOpenDeleteConfigurationDialog(false)}
      />
    </Container>
  )
}

export default AccessRequestConfiguration

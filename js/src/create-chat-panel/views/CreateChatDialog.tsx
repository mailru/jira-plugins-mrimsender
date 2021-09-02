import React from 'react';
import Modal, {ContainerComponentProps, ModalFooter} from '@atlaskit/modal-dialog';
import {I18n} from '@atlassian/wrm-react-i18n';
import Form, {ErrorMessage, Field} from '@atlaskit/form';
import Textfield from '@atlaskit/textfield';
import Button, {ButtonGroup} from '@atlaskit/button';
import {ValueType} from '@atlaskit/select';
import {ChatCreationData} from '../stores/ChatPanelStore';
import UserPicker, {OptionData, Value} from '@atlaskit/user-picker';

type ChatDialogProps = {
    chatCreationData: ChatCreationData;
    closeDialog(): void;
    createChat(name: string, memberIds: number[]): void;
    loadUsers(): Promise<OptionData | OptionData[]>
};

type CreateChatDialogFormValuesType = {
    'chat-name': string;
    'chat-members': ValueType<OptionData, true>;
};

const validateChatName = (value?: string) => {
    if (!value || value.trim().length == 0) {
        return 'TOO_SHORT';
    }
};

const validateChatMembers = (value: ValueType<OptionData, true>) => {
    if (!value || value.length <= 1) {
        return 'TOO_SHORT';
    }
    if (value && value.length > 30) {
        return 'TOO_MORE';
    }
};

export const CreateChatDialog = (props: ChatDialogProps) => {
    const {closeDialog, chatCreationData, createChat, loadUsers} = props;
    return (
        <Modal
            onClose={closeDialog}
            heading={I18n.getText('ru.mail.jira.plugins.myteam.createChat.panel.title')}
            scrollBehavior={'outside'}
            components={{
                Container: ({children, className}: ContainerComponentProps) => (
                    <Form<CreateChatDialogFormValuesType>
                        onSubmit={(values) => {
                            if (values != null && values['chat-members'] != null && values['chat-name'] != null) {
                                createChat(
                                    values['chat-name'],
                                    values['chat-members'].map((member) => Number.parseInt(member.id)),
                                );
                                closeDialog();
                            }
                        }}>
                        {({formProps}) => (
                            <form {...formProps} className={className}>
                                {children}
                            </form>
                        )}
                    </Form>
                ),
                Footer: (props) => (
                    <ModalFooter showKeyline={props.showKeyline}>
                        <span/>
                        <ButtonGroup>
                            <Button appearance="subtle" onClick={props.onClose}>
                                {I18n.getText('common.forms.cancel')}
                            </Button>
                            <Button appearance="primary" type="submit">
                                {I18n.getText('common.forms.create')}
                            </Button>
                        </ButtonGroup>
                    </ModalFooter>
                ),
            }}>
            <Field
                label={I18n.getText('ru.mail.jira.plugins.myteam.createChat.panel.name')}
                name="chat-name"
                defaultValue={chatCreationData.name}
                validate={validateChatName}>
                {({fieldProps, valid, error}) => (
                    <>
                        <Textfield {...fieldProps} />
                        {error === 'TOO_SHORT' && !valid && (
                            <ErrorMessage>
                                {I18n.getText('ru.mail.jira.plugins.myteam.createChat.panel.error.empty_name')}
                            </ErrorMessage>
                        )}
                    </>
                )}
            </Field>

            <Field<ValueType<OptionData, true>>
                name="chat-members"
                label={I18n.getText('ru.mail.jira.plugins.myteam.createChat.panel.members')}
                defaultValue={chatCreationData.members}
                validate={validateChatMembers}>
                {({fieldProps: {id, ...rest}, error, valid}) => (
                    <>
                        <UserPicker
                            {...rest}
                            width={'100%'}
                            value={rest.value as Value}
                            onChange={(value) => {
                                rest.onChange(value as ValueType<OptionData, true>);
                            }}
                            inputId={id}
                            loadOptions={loadUsers}
                            isMulti
                            fieldId={null}
                        />
                        {error === 'TOO_SHORT' && !valid && (
                            <ErrorMessage>
                                {I18n.getText('ru.mail.jira.plugins.myteam.createChat.panel.error.empty_members')}
                            </ErrorMessage>
                        )}
                        {error === 'TOO_MORE' && !valid && (
                            <ErrorMessage>
                                {I18n.getText('ru.mail.jira.plugins.myteam.createChat.panel.error.more_members')}
                            </ErrorMessage>
                        )}
                    </>
                )}
            </Field>
        </Modal>
    );
};

import { Snackbar } from '@vkontakte/vkui'
import React, {
  createContext,
  useState,
  useEffect,
  useMemo,
  ReactNode,
  Dispatch,
  SetStateAction,
} from 'react'

import './MessageProvider.pcss'

export type MessageType = 'neutral' | 'positive' | 'negative'

type MessageContextType = {
  message: string
  setMessage: Dispatch<SetStateAction<string>>
  messageType: MessageType
  setMessageType: Dispatch<SetStateAction<MessageType>>
  setIsVisible: Dispatch<SetStateAction<boolean>>
}

export const MessageContext = createContext<MessageContextType | null>(null)

const MessageProvider = ({ children }: { children: ReactNode }) => {
  const [message, setMessage] = useState('')
  const [messageType, setMessageType] = useState<MessageType>('neutral')
  const [isVisible, setIsVisible] = useState(false)

  // eslint-disable-next-line consistent-return
  useEffect(() => {
    if (isVisible) {
      const timer = setTimeout(() => {
        setIsVisible(false)
        setMessage('')
        setMessageType('neutral')
      }, 5000)
      return () => clearTimeout(timer)
    }
  }, [isVisible])

  const messageContextValue = useMemo(
    () => ({
      message,
      setMessage,
      messageType,
      setMessageType,
      setIsVisible,
    }),
    [message, setMessage, messageType, setMessageType, setIsVisible]
  )

  const classes = ['status-message', messageType].join(' ')

  return (
    <MessageContext.Provider value={messageContextValue}>
      {children}
      {isVisible ? (
        <Snackbar className={classes} onClose={() => setIsVisible(false)}>
          {message}
        </Snackbar>
      ) : null}
    </MessageContext.Provider>
  )
}

export default MessageProvider

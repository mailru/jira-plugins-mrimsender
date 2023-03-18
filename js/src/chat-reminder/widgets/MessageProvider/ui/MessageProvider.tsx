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

type MessageContextType = {
  message: string
  setMessage: Dispatch<SetStateAction<string>>
}

export const MessageContext = createContext<MessageContextType | null>(null)

const MessageProvider = ({ children }: { children: ReactNode }) => {
  const [message, setMessage] = useState('')

  const [snackbar, setSnackbar] = React.useState<ReactNode>(null)

  useEffect(() => {
    if (message && message.trim().length > 0) {
      setSnackbar(
        <Snackbar className="status-message" onClose={() => setSnackbar(null)}>
          {message}
        </Snackbar>
      )
    }
  }, [message])

  const messageContextValue = useMemo(
    () => ({
      message,
      setMessage,
    }),
    [message, setMessage]
  )

  return (
    <MessageContext.Provider value={messageContextValue}>
      {children}
      {snackbar}
    </MessageContext.Provider>
  )
}

export default MessageProvider

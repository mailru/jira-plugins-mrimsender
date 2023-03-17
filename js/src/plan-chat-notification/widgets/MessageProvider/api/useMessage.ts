// useMessage.js
import { useContext } from 'react'
import { MessageContext } from '../ui/MessageProvider'

const useMessage = () => {
  const context = useContext(MessageContext)

  if (!context) {
    throw new Error('useMessage must be used within a MessageProvider')
  }
  const { message, setMessage } = context

  const showMessage = (text: string) => {
    setMessage(text)
  }

  return { message, showMessage }
}

export default useMessage

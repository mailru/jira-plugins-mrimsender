// useMessage.js
import { useContext } from 'react'
import { MessageContext, MessageType } from '../ui/MessageProvider'

const useMessage = () => {
  const context = useContext(MessageContext)

  if (!context) {
    throw new Error('useMessage must be used within a MessageProvider')
  }
  const { message, setMessage, setMessageType, setIsVisible } = context

  const showMessage = (
    text: string,
    type: MessageType = 'neutral',
    duration = 5000
  ) => {
    setMessage(text)
    setMessageType(type)
    setIsVisible(true)
    setTimeout(() => {
      setIsVisible(false)
      setMessage('')
      setMessageType('neutral')
    }, duration)
  }

  return { message, showMessage }
}

export default useMessage

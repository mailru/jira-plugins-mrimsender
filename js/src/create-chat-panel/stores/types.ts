import { AvatarProps } from '@atlaskit/avatar-group'

type ChatMember = {
  id: string
  name: string
  src: string
}

export type ChatCreationData = {
  name: string
  description: string
  members: ChatMember[]
}

export type ChatInfoType = {
  link: string
  name: string
  members: AvatarProps[]
}

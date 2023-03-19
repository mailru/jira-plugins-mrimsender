import React, { useEffect, useRef } from 'react'
import {
  QueryObserverResult,
  useMutation,
  UseMutationResult,
  useQuery,
} from 'react-query'
import { AxiosError } from 'axios'
import {
  AccessRequest,
  AccessRequestConfiguration,
  ErrorData,
  FilterSubscription,
  FilterSubscriptionsPermissions,
} from './types'
import {
  createSubscription,
  deleteSubscription,
  getCurrentUserSubscriptions,
  getSubscriptionsPermissions,
  runSubscription,
  updateSubscription,
} from './api/SubscriptionsApiClient'
import {
  createAccessRequestConfiguration,
  deleteAccessRequestConfiguration,
  getAccessRequest,
  getAccessRequestConfiguration,
  sendAccessRequest,
  updateAccessRequestConfiguration,
} from './api/AccessRequestApiClient'

export const useTimeoutState = <T>(
  defaultState: T
): [T, (action: React.SetStateAction<T>, timeout: number) => void] => {
  const [state, setState] = React.useState<T>(defaultState)
  const [currentTimeoutId, setCurrentTimeoutId] =
    React.useState<NodeJS.Timeout>()

  const setTimeoutState = React.useCallback(
    (action: React.SetStateAction<T>, timeout: number) => {
      if (currentTimeoutId != null) {
        clearTimeout(currentTimeoutId)
      }

      setState(action)

      const id = setTimeout(() => setState(defaultState), timeout ?? 4000)
      setCurrentTimeoutId(id as any)
    },
    [currentTimeoutId, defaultState]
  )
  return [state, setTimeoutState]
}

export const usePrevious = <T>(value: T): T | undefined => {
  const ref = useRef<T>()
  useEffect(() => {
    ref.current = value
  })
  return ref.current
}

export const useGetSubscriptions = (params?: {
  subscribers?: Array<String>
  filterId?: number
  recipientsType?: string
  recipients?: Array<String>
}): QueryObserverResult<FilterSubscription[], AxiosError> =>
  useQuery<FilterSubscription[], AxiosError>(
    ['getSubscriptions', params],
    () => getCurrentUserSubscriptions(params),
    {
      refetchOnWindowFocus: false,
      retry: false,
    }
  )

export const useSubscriptionMutation = (): UseMutationResult<
  undefined,
  AxiosError<ErrorData>,
  FilterSubscription
> =>
  useMutation((subscription: FilterSubscription) =>
    subscription.id
      ? updateSubscription(subscription, subscription.id)
      : createSubscription(subscription)
  )

export const useSubscriptionDelete = (): UseMutationResult<
  undefined,
  AxiosError,
  number
> => useMutation((id: number) => deleteSubscription(id))

export const useRunSubscriptionMutation = (): UseMutationResult<
  undefined,
  AxiosError,
  number
> => useMutation((id: number) => runSubscription(id))

export const useGetSubscriptionsPermissions = (): QueryObserverResult<
  FilterSubscriptionsPermissions,
  AxiosError
> =>
  useQuery<FilterSubscriptionsPermissions, AxiosError>(
    ['getSubscriptionsPermissions'],
    () => getSubscriptionsPermissions(),
    {
      refetchOnWindowFocus: false,
      retry: false,
    }
  )

export const useGetAccessRequest = (
  issueKey: string
): QueryObserverResult<AccessRequest, AxiosError> =>
  useQuery<AccessRequest, AxiosError>(
    ['getAccessRequestConfiguration', issueKey],
    () => getAccessRequest(issueKey),
    {
      refetchOnWindowFocus: false,
      retry: false,
    }
  )

export const useAccessRequestMutation = (): UseMutationResult<
  undefined,
  AxiosError,
  { issueKey: string; accessRequest: AccessRequest }
> =>
  useMutation(
    ({
      issueKey,
      accessRequest,
    }: {
      issueKey: string
      accessRequest: AccessRequest
    }) => sendAccessRequest(issueKey, accessRequest)
  )

export const useGetAccessRequestConfiguration = (
  projectKey: string
): QueryObserverResult<AccessRequestConfiguration, AxiosError> =>
  useQuery<AccessRequestConfiguration, AxiosError>(
    ['getAccessRequestConfiguration', projectKey],
    () => getAccessRequestConfiguration(projectKey),
    {
      refetchOnWindowFocus: false,
      retry: false,
    }
  )

export const useAccessRequestConfigurationMutation = (): UseMutationResult<
  undefined,
  AxiosError<ErrorData>,
  AccessRequestConfiguration
> =>
  useMutation((configuration: AccessRequestConfiguration) =>
    configuration.id
      ? updateAccessRequestConfiguration(configuration, configuration.id)
      : createAccessRequestConfiguration(configuration)
  )

export const useAccessRequestConfigurationDelete = (): UseMutationResult<
  undefined,
  AxiosError,
  {
    projectKey: string
    id: number
  }
> =>
  useMutation(({ projectKey, id }: { projectKey: string; id: number }) =>
    deleteAccessRequestConfiguration(projectKey, id)
  )

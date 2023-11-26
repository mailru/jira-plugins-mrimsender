import axios from 'axios'
import contextPath from 'wrm/context-path'
import qs from 'qs'
import { FilterSubscription, FilterSubscriptionsPermissions } from '../types'

export const subscriptionsPath = (): string => {
  return `${contextPath()}/rest/myteam/1.0/subscriptions`
}

export const getCurrentUserSubscriptions = (params?: {
  subscribers?: Array<String>
  filterId?: number
  recipientsType?: string
  recipients?: Array<String>
}): Promise<FilterSubscription[]> =>
  axios
    .get(`${subscriptionsPath()}`, {
      params,
      paramsSerializer: (params) => {
        return qs.stringify(params, { arrayFormat: 'repeat' })
      },
    })
    .then((response) => response.data)

export const createSubscription = (subscription: FilterSubscription) =>
  axios
    .post(`${subscriptionsPath()}`, subscription)
    .then((response) => response.data)

export const updateSubscription = (
  subscription: FilterSubscription,
  subscriptionId: number
) =>
  axios
    .put(`${subscriptionsPath()}/${subscriptionId}`, subscription)
    .then((response) => response.data)

export const deleteSubscription = (subscriptionId: number): Promise<any> =>
  axios
    .delete(`${subscriptionsPath()}/${subscriptionId}`)
    .then((response) => response.data)

export const runSubscription = (subscriptionId: number): Promise<any> =>
  axios
    .post(`${subscriptionsPath()}/${subscriptionId}`)
    .then((response) => response.data)

export const getSubscriptionsPermissions =
  (): Promise<FilterSubscriptionsPermissions> =>
    axios
      .get(`${contextPath()}/rest/myteam/1.0/subscriptions/permissions`)
      .then((response) => response.data)

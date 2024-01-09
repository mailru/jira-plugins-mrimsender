import axios from 'axios'
import contextPath from 'wrm/context-path'
// eslint-disable-next-line import/no-extraneous-dependencies
import qs from 'qs'
import { FilterSubscription, FilterSubscriptionsPermissions } from '../types'

const ENDPOINTS = {
  SUBSCRIPTION: `${contextPath()}/rest/myteam/1.0/subscriptions`,
  SUB_URL_BUILD: function build(endpointParam: any): string {
    return `${this.SUBSCRIPTION}/${endpointParam}`
  },
}

export const getCurrentUserSubscriptions = (params?: {
  subscribers?: Array<String>
  filterId?: number
  recipientsType?: string
  recipients?: Array<String>
}): Promise<FilterSubscription[]> =>
  axios
    .get(ENDPOINTS.SUBSCRIPTION, {
      params,
      paramsSerializer: (paramsSerializer) => {
        return qs.stringify(paramsSerializer, { arrayFormat: 'repeat' })
      },
    })
    .then((response) => response.data)

export const createSubscription = (subscription: FilterSubscription) =>
  axios
    .post(ENDPOINTS.SUBSCRIPTION, subscription)
    .then((response) => response.data)

export const updateSubscription = (
  subscription: FilterSubscription,
  subscriptionId: number
) =>
  axios
    .put(ENDPOINTS.SUB_URL_BUILD(subscriptionId), subscription)
    .then((response) => response.data)

export const deleteSubscription = (subscriptionId: number): Promise<any> =>
  axios
    .delete(ENDPOINTS.SUB_URL_BUILD(subscriptionId))
    .then((response) => response.data)

export const runSubscription = (subscriptionId: number): Promise<any> =>
  axios
    .post(ENDPOINTS.SUB_URL_BUILD(`${subscriptionId}/run`))
    .then((response) => response.data)

export const getSubscriptionsPermissions =
  (): Promise<FilterSubscriptionsPermissions> =>
    axios
      .get(ENDPOINTS.SUB_URL_BUILD('permissions'))
      .then((response) => response.data)

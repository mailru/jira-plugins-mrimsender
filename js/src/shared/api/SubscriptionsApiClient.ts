import axios from 'axios';
import contextPath from 'wrm/context-path';
import { FilterSubscription, FilterSubscriptionsPermissions } from '../types';
import qs from 'qs';

export const getCurrentUserSubscriptions = (params?: {
  subscribers?: Array<String>;
  filterId?: number;
  recipientsType?: string;
  recipients?: Array<String>;
}): Promise<FilterSubscription[]> =>
  axios
    .get(`${contextPath()}/rest/myteam/1.0/subscriptions`, {
      params,
      paramsSerializer: (params) => {
        return qs.stringify(params, { arrayFormat: 'repeat' });
      },
    })
    .then((response) => response.data);

export const createSubscription = (subscription: FilterSubscription) =>
  axios
    .post(`${contextPath()}/rest/myteam/1.0/subscriptions`, subscription)
    .then((response) => response.data);

export const updateSubscription = (
  subscription: FilterSubscription,
  subscriptionId: number,
) =>
  axios
    .put(
      `${contextPath()}/rest/myteam/1.0/subscriptions/${subscriptionId}`,
      subscription,
    )
    .then((response) => response.data);

export const deleteSubscription = (subscriptionId: number): Promise<any> =>
  axios
    .delete(`${contextPath()}/rest/myteam/1.0/subscriptions/${subscriptionId}`)
    .then((response) => response.data);

export const runSubscription = (subscriptionId: number): Promise<any> =>
  axios
    .post(
      `${contextPath()}/rest/myteam/1.0/subscriptions/${subscriptionId}/run`,
    )
    .then((response) => response.data);

export const getSubscriptionsPermissions =
  (): Promise<FilterSubscriptionsPermissions> =>
    axios
      .get(`${contextPath()}/rest/myteam/1.0/subscriptions/permissions`)
      .then((response) => response.data);

import axios from 'axios';
import contextPath from 'wrm/context-path';
import { FilterSubscription } from '../types';

export const getCurrentUserSubscriptions = (): Promise<FilterSubscription[]> =>
  axios
    .get(`${contextPath()}/rest/myteam/1.0/subscriptions`)
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

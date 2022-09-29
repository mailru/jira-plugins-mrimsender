import axios from 'axios';
import contextPath from 'wrm/context-path';
import { FilterSubscription } from '../types';

export const getCurrentUserSubscriptions = (): Promise<FilterSubscription[]> =>
  axios
    .get(`${contextPath()}/rest/myteam/1.0/subscriptions`)
    .then((response) => response.data);

export const deleteSubscription = (subscriptionId: number): Promise<any> =>
  axios
    .delete(`${contextPath()}/rest/myteam/1.0/subscriptions/${subscriptionId}`)
    .then((response) => response.data);
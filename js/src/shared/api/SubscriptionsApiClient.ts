import axios, { AxiosResponse } from 'axios';
import contextPath from 'wrm/context-path';
import { FilterSubscription } from '../types';

export const loadUserSubscriptions = async (): Promise<
  AxiosResponse<Array<FilterSubscription>>
> => {
  return axios.get(`${contextPath()}/rest/myteam/1.0/subscriptions`);
};

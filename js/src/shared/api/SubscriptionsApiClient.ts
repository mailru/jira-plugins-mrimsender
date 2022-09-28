import axios from 'axios';
import contextPath from 'wrm/context-path';
import { FilterSubscription } from '../types';

export const getCurrentUserSubscriptions = (): Promise<FilterSubscription[]> =>
  axios
    .get(`${contextPath()}/rest/myteam/1.0/subscriptions`)
    .then((response) => response.data);

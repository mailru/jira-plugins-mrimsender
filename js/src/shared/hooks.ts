import React, { useEffect, useRef } from 'react';
import {
  QueryObserverResult,
  useMutation,
  UseMutationResult,
  useQuery,
} from 'react-query';
import { AxiosError } from 'axios';
import { ErrorData, FilterSubscription } from './types';
import {
  createSubscription,
  deleteSubscription,
  getCurrentUserSubscriptions,
  updateSubscription,
} from './api/SubscriptionsApiClient';

export const useTimeoutState = <T>(
  defaultState: T,
): [T, (action: React.SetStateAction<T>, timeout: number) => void] => {
  const [state, setState] = React.useState<T>(defaultState);
  const [currentTimeoutId, setCurrentTimeoutId] =
    React.useState<NodeJS.Timeout>();

  const setTimeoutState = React.useCallback(
    (action: React.SetStateAction<T>, timeout: number) => {
      if (currentTimeoutId != null) {
        clearTimeout(currentTimeoutId);
      }

      setState(action);

      const id = setTimeout(() => setState(defaultState), timeout ?? 4000);
      setCurrentTimeoutId(id as any);
    },
    [currentTimeoutId, defaultState],
  );
  return [state, setTimeoutState];
};

export const usePrevious = <T>(value: T): T | undefined => {
  const ref = useRef<T>();
  useEffect(() => {
    ref.current = value;
  });
  return ref.current;
};

export const useGetSubscriptions = (): QueryObserverResult<
  FilterSubscription[],
  AxiosError
> =>
  useQuery<FilterSubscription[], AxiosError>(
    ['getSubscriptions'],
    () => getCurrentUserSubscriptions(),
    {
      refetchOnWindowFocus: false,
      retry: false,
    },
  );

export const useSubscriptionMutation = (): UseMutationResult<
  undefined,
  AxiosError<ErrorData>,
  FilterSubscription
> =>
  useMutation((subscription: FilterSubscription) =>
    subscription.id
      ? updateSubscription(subscription, subscription.id)
      : createSubscription(subscription),
  );

export const useSubscriptionDelete = (): UseMutationResult<
  undefined,
  AxiosError,
  number
> => useMutation((id: number) => deleteSubscription(id));

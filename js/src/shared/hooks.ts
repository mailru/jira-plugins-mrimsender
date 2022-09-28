import React, { useEffect, useRef } from 'react';
import {QueryObserverResult, useQuery} from "react-query";
import {AxiosError} from "axios";
import {FilterSubscription} from "./types";
import {getCurrentUserSubscriptions} from "./api/SubscriptionsApiClient";

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

export const useGetSubscriptions = (): QueryObserverResult<FilterSubscription[], AxiosError> =>
  useQuery<FilterSubscription[], AxiosError> (
    ['getSubscriptions'],
    () => getCurrentUserSubscriptions(),
    {
      refetchOnWindowFocus: false,
      retry: false,
    },
  );
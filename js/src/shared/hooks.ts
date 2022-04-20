import React, { useEffect, useRef } from 'react';

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

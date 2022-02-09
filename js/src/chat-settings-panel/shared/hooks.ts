import React from 'react';

export const useTimeoutState = <T>(
  defaultState: T,
): [T, (action: React.SetStateAction<T>, timeout: number) => void] => {
  const [state, setState] = React.useState<T>(defaultState);
  const [currentTimeoutId, setCurrentTimeoutId] = React.useState<NodeJS.Timeout | undefined>();

  const setTimeoutState = React.useCallback(
    (action: React.SetStateAction<T>, timeout: number) => {
      if (currentTimeoutId != null) {
        clearTimeout(currentTimeoutId);
      }

      setState(action);

      const id = setTimeout(() => setState(defaultState), timeout ?? 4000);
      setCurrentTimeoutId(id);
    },
    [currentTimeoutId, defaultState],
  );
  return [state, setTimeoutState];
};

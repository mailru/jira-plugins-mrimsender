import axios, { CancelToken, CancelTokenSource } from 'axios';

const CancelToken = axios.CancelToken;

export function getCancelTokenHandler(): (tokenId: string) => CancelTokenSource {
  const cancelTokenCache: { [key: string]: CancelTokenSource } = {};

  return (tokenName: string) => {
    const prevToken = cancelTokenCache[tokenName];
    if (prevToken) {
      prevToken.cancel(`${tokenName} canceled`);
    }
    cancelTokenCache[tokenName] = CancelToken.source();
    return cancelTokenCache[tokenName];
  };
}

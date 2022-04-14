import styled from '@emotion/styled';
import React, { ReactElement, ReactNode } from 'react';
import Spinner from '@atlaskit/spinner';
import SectionMessage from '@atlaskit/section-message';

type Props = {
  className?: string;
  children?: ReactNode;
  isLoading?: boolean;
  error?: string;
};

const Container = styled.div`
  display: flex;
  place-items: center;
  flex-direction: column;
`;

function LoadableComponent({
  className,
  children,
  isLoading,
  error,
}: Props): ReactElement {
  return (
    <Container className={className}>
      {isLoading ? <Spinner size="large" /> : children}
      {error ? (
        <SectionMessage appearance="error">
          <p>{error}</p>
        </SectionMessage>
      ) : null}
    </Container>
  );
}

LoadableComponent.defaultProps = {
  className: undefined,
  children: undefined,
  isLoading: undefined,
  error: undefined,
};

export default LoadableComponent;

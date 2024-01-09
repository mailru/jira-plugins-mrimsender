import styled from 'styled-components'

const EmptyPageContainer = styled.div<{ h4?: number; p?: number }>`
  margin: 48px auto;
  text-align: center;
  width: 500px;

  h4 {
    margin-bottom: ${(props) => props.h4 || 0}px;
  }

  p {
    margin-bottom: ${(props) => props.p || 0}px;
  }
`

export default EmptyPageContainer

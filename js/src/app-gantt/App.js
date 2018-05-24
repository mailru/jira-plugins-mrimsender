//@flow
import React, {Fragment} from 'react';
import {connect} from 'react-redux';

import memoizeOne from 'memoize-one';

import {GanttComponent} from './gantt/GanttComponent';
import {GanttActions} from './GanttActions';
import type {DhtmlxGantt} from './gantt/types';


type State = {
    gantt: ?DhtmlxGantt
};

const ConnectedGanttComponent = connect(
    memoizeOne(({options, calendar, isLoading}) => ({options, calendar, isLoading}))
)(GanttComponent);

export class App extends React.PureComponent<{}, State> {
    state = {
        gantt: null
    };

    _setGantt = (gantt: DhtmlxGantt) => this.setState({ gantt });

    render() {
        return (
            <Fragment>
                {/* $FlowFixMe weird error with connected component */}
                <GanttActions gantt={this.state.gantt}/>
                <ConnectedGanttComponent onGanttInit={this._setGantt}/>
            </Fragment>
        )
    }
}

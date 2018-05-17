//@flow
import React, {Fragment} from 'react';

import {connect} from 'react-redux';

import memoizeOne from 'memoize-one';

import DropdownMenu, {DropdownItemGroupCheckbox, DropdownItemCheckbox} from '@atlaskit/dropdown-menu';
import Badge from '@atlaskit/badge';

import FilterIcon from '@atlaskit/icon/glyph/filter';

import type {CurrentCalendarType} from './types';
import {calendarService} from '../service/services';
import {CalendarActionCreators} from '../service/gantt.reducer';


type Props = {
    calendar: CurrentCalendarType,
    selectFilter: typeof CalendarActionCreators.selectFilter
};

class QuickFiltersInternal extends React.PureComponent<Props> {
    _toggleFilter = (id: number) => () => {
        const filter = this.props.calendar.favouriteQuickFilters.find(it => it.id === id);
        if (filter) {
            const newVal = !filter.selected;
            calendarService
                .selectQuickFilter(this.props.calendar.id, id, newVal)
                .then(() => this.props.selectFilter(id, newVal))
        }
    };

    render() {
        const {calendar} = this.props;

        const quickFilters = calendar ? this.props.calendar.favouriteQuickFilters : [];
        let selectedFilters = 0;
        for (const filter of quickFilters) {
            if (filter.selected) {
                selectedFilters++;
            }
        }

        const trigger = (
            <Fragment>
                Фильтры
            </Fragment>
        );

        return (
            <DropdownMenu
                trigger={trigger}
                triggerType="button"
                triggerButtonProps={{
                    iconBefore: <FilterIcon label="filter"/>,
                    iconAfter: <Badge appearance={selectedFilters > 0 ? 'primary' : 'default'} value={selectedFilters}/>,
                    isDisabled: quickFilters.length === 0
                }}
                shouldFlip={false}
                position="bottom right"
                boundariesElement="window"
            >
                <DropdownItemGroupCheckbox id="quick-filters">
                    {quickFilters.map(filter =>
                        <DropdownItemCheckbox
                            key={filter.id}
                            id={filter.id.toString()}
                            isSelected={filter.selected}
                            onClick={this._toggleFilter(filter.id)}
                        >
                            {filter.name}
                        </DropdownItemCheckbox>
                    )}
                </DropdownItemGroupCheckbox>
            </DropdownMenu>
        );
    }
}

export const QuickFilters = connect(
    memoizeOne(({calendar}) => ({calendar})),
    { selectFilter: CalendarActionCreators.selectFilter }
)(QuickFiltersInternal);

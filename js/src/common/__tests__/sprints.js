import React from 'react';
import { shallow } from 'enzyme';

import { SprintState } from '../sprints';
import Lozenge from '@atlaskit/lozenge';

describe('<SprintState />', () => {
    it('renders FUTURE <SprintState /> components', () => {
        const wrapper = shallow(<SprintState state={'FUTURE'} />);
        expect(wrapper.find(Lozenge).exists()).toBeTruthy();
        expect(wrapper.find(Lozenge).prop('appearance')).toBe('new');
    });

    it('renders ACTIVE <SprintState /> components', () => {
        const wrapper = shallow(<SprintState state={'ACTIVE'} />);
        expect(wrapper.find(Lozenge).exists()).toBeTruthy();
        expect(wrapper.find(Lozenge).prop('appearance')).toBe('inprogress');
    });

    it('renders CLOSED <SprintState /> components', () => {
        const wrapper = shallow(<SprintState state={'CLOSED'} />);
        expect(wrapper.find(Lozenge).exists()).toBeTruthy();
        expect(wrapper.find(Lozenge).prop('appearance')).toBe('default');
    });

    it('renders unknown <SprintState /> components', () => {
        const wrapper = shallow(<SprintState state={'TEST_STATE'} />);
        expect(wrapper.find(Lozenge).exists()).toBeTruthy();
        expect(wrapper.find(Lozenge).prop('appearance')).toBe('default');
    });
});
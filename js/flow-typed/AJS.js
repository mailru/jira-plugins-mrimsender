//@flow
declare module 'AJS' {
    declare type InlineDialogCallback = (content: JQuery, trigger: JQuery, showPopup: Function) => void;
    declare type CalculatePositionsFunction = (popup: any, targetPosition: {target: JQuery}, mousePosition: {x: number, y: number}, opts: {}) => any;

    declare interface InlineDialogInstance {
        hide(): void,
        refresh(): void
    }

    declare type InlineDialogOptions = {
        calculatePositions: CalculatePositionsFunction
    }

    declare interface InlineDialog {
        (query: string, identifier: string, callback: InlineDialogCallback, opts: InlineDialogOptions): InlineDialogInstance;
        opts: {
            calculatePositions: CalculatePositionsFunction
        }
    }

    declare type FlagParameters = {
        title?: string,
        type?: 'success' | 'info' | 'warning' | 'error',
        body?: string,
        close?: 'auto' | 'manual' | 'never'
    };

    declare type FlagType = {
        close(): void
    };

    declare export default {
        contextPath(): string;
        flag(FlagParameters): FlagType;
        toInit(() => void): void;
        dim(): void;
        undim(): void;
        $: JQueryStatic;
        InlineDialog: InlineDialog;
    };
}

declare module 'JIRA' {
    declare export default {
        Loading: {
            showLoadingIndicator(): void,
            hideLoadingIndicator(): void
        },
        Templates: any
    }
}

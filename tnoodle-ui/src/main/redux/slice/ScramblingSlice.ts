import { createSlice, PayloadAction } from "@reduxjs/toolkit";
import WebsocketBlobResult from "../../model/WebsocketBlobResult";

interface ScramblingState {
    fileZip?: WebsocketBlobResult;
    generatingScrambles: boolean;
    isValidSignedBuild: boolean;
    isAllowedVersion: boolean;
    password: string;
    scramblingProgressCurrent: Record<string, number>;
    scramblingProgressTarget: Record<string, number>;
}

const initialState: ScramblingState = {
    fileZip: undefined,
    generatingScrambles: false,
    isValidSignedBuild: false,
    isAllowedVersion: false,
    password: "",
    scramblingProgressCurrent: {},
    scramblingProgressTarget: {},
};

export const scramblingSlice = createSlice({
    name: "scramblingSlice",
    initialState,
    reducers: {
        setFileZip: (
            state,
            action: PayloadAction<WebsocketBlobResult | undefined>
        ) => {
            state.fileZip = action.payload;
        },
        setGeneratingScrambles: (state, action: PayloadAction<boolean>) => {
            state.generatingScrambles = action.payload;
        },
        setValidSignedBuild: (state, action: PayloadAction<boolean>) => {
            state.isValidSignedBuild = action.payload;
        },
        setAllowedVersion: (state, action: PayloadAction<boolean>) => {
            state.isAllowedVersion = action.payload;
        },
        setPassword: (state, action: PayloadAction<string>) => {
            state.password = action.payload;
        },
        resetScramblingProgressCurrent: (state) => {
            state.scramblingProgressCurrent = {};
        },
        setScramblingProgressCurrentEvent: (
            state,
            action: PayloadAction<string>
        ) => {
            state.scramblingProgressCurrent[action.payload] =
                (state.scramblingProgressCurrent[action.payload] || 0) + 1;
        },
        setScramblingProgressTarget: (
            state,
            action: PayloadAction<Record<string, number>>
        ) => {
            state.scramblingProgressTarget = action.payload;
        },
    },
});

export const {
    setFileZip,
    setValidSignedBuild,
    setAllowedVersion,
    setPassword,
    setGeneratingScrambles,
    resetScramblingProgressCurrent,
    setScramblingProgressCurrentEvent,
    setScramblingProgressTarget,
} = scramblingSlice.actions;

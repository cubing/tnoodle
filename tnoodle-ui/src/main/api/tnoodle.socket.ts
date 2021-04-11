import { tNoodleBackend } from "./tnoodle.api";
import WebsocketBlobResult from "../model/WebsocketBlobResult";

export type ScrambleHandshakeFn = (payload: Record<string, number>) => void;
export type ScrambleProgressFn = (payload: string) => void;

enum ScramblingState {
    Idle,
    Initiate,
    Scrambling,
    ComputingType,
    ComputingData,
    Done,
    Error = -1,
}

export class ScrambleClient {
    onHandshake: ScrambleHandshakeFn;
    onProgress: ScrambleProgressFn;

    state: ScramblingState;

    contentType: string;

    resultPayload: string;
    errorPayload: any;

    constructor(
        onHandshake: ScrambleHandshakeFn,
        onProgress: ScrambleProgressFn
    ) {
        this.onHandshake = onHandshake;
        this.onProgress = onProgress;

        this.state = ScramblingState.Idle;

        this.contentType = FALLBACK_APPLICATION_TYPE;

        this.resultPayload = "";
        this.errorPayload = null;
    }

    loadScrambles(
        endpoint: String,
        payload: object,
        targetMarker: String
    ): Promise<WebsocketBlobResult> {
        return new Promise((resolve, reject) => {
            let ws = new WebSocket(BASE_URL + endpoint);

            ws.onopen = () => {
                this.state = ScramblingState.Initiate;
                ws.send(JSON.stringify(payload));
            };

            ws.onerror = () => {
                reject(this.errorPayload);
            };

            ws.onclose = (cls) => {
                if (this.state === ScramblingState.Done && cls.wasClean) {
                    let resultObject = {
                        contentType: this.contentType,
                        payload: this.resultPayload,
                    };

                    resolve(resultObject);
                } else {
                    reject(this.errorPayload);
                }
            };

            ws.onmessage = (msg) => {
                if (this.state === ScramblingState.Error) {
                    let rawPayload = msg.data.toString();
                    this.errorPayload = JSON.parse(rawPayload);
                }

                if (msg.data === MARKER_ERROR_MESSAGE) {
                    this.state = ScramblingState.Error; // purposefully go into an error state
                }

                if (this.state === ScramblingState.Initiate) {
                    this.state = ScramblingState.Scrambling;

                    let rawPayload = msg.data.toString();
                    let targetPayload = JSON.parse(rawPayload);

                    this.onHandshake(targetPayload);
                } else if (this.state === ScramblingState.Scrambling) {
                    if (msg.data === targetMarker) {
                        this.state = ScramblingState.ComputingType;
                    } else {
                        this.onProgress(msg.data);
                    }
                } else if (this.state === ScramblingState.ComputingType) {
                    this.state = ScramblingState.ComputingData;

                    this.contentType = msg.data;
                } else if (this.state === ScramblingState.ComputingData) {
                    this.state = ScramblingState.Done;

                    this.resultPayload = msg.data;
                }
            };
        });
    }
}

let wsTNoodleBackend = new URL(tNoodleBackend);
wsTNoodleBackend.protocol =
    window.location.protocol === "https:" ? "wss" : "ws";

const FALLBACK_APPLICATION_TYPE = "application/octet-stream";

const BASE_URL = wsTNoodleBackend.toString().replace(/\/$/g, "");

// this has to be identical with backend value in JobSchedulingHandler.kt
const MARKER_ERROR_MESSAGE = "%%ERROR%%";

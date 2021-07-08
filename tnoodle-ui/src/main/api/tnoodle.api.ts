import Axios from "axios";
import BestMbld from "../model/BestMbld";
import RunningVersion from "../model/RunningVersion";
import Translation from "../model/Translation";
import WcaEvent from "../model/WcaEvent";
import WcaFormat from "../model/WcaFormat";
import Wcif from "../model/Wcif";
import { ScrambleClient } from "./tnoodle.socket";
import WebsocketBlobResult from "../model/WebsocketBlobResult";
import FrontendStatus from "../model/FrontendStatus";

let backendUrl = new URL("http://localhost:2014");
export const tNoodleBackend = backendUrl.toString().replace(/\/$/g, "");

let zipEndpoint = "/wcif/zip";
let versionEndpoint = "/version";
let fmcTranslationsEndpoint = "/frontend/fmc/languages/available";
let suggestedFmcTranslationsEndpoint = "/frontend/fmc/languages/competitors";
let bestMbldAttemptEndpoint = "/frontend/mbld/best";
let wcaEventsEndpoint = "/frontend/data/events";
let formatsEndpoint = "/frontend/data/formats";

/**
 * Builds the object expected for FMC translations
 * @param {array} translations e.g. ["de", "da", "pt-BR"]
 */
const fmcTranslationsHelper = (translations?: Translation[]) => {
    if (!translations) {
        return null;
    }
    return {
        languageTags: translations
            .filter((translation) => translation.status)
            .map((translation) => translation.id),
    };
};

class TnoodleApi {
    fetchWcaEvents = () =>
        Axios.get<WcaEvent[]>(tNoodleBackend + wcaEventsEndpoint);

    fetchFormats = () =>
        Axios.get<Record<string, WcaFormat>>(tNoodleBackend + formatsEndpoint);

    fetchSuggestedFmcTranslations = (wcif: Wcif) =>
        Axios.post<string[]>(
            tNoodleBackend + suggestedFmcTranslationsEndpoint,
            wcif
        );

    fetchBestMbldAttempt = (wcif: Wcif) =>
        Axios.post<BestMbld>(tNoodleBackend + bestMbldAttemptEndpoint, wcif);

    fetchRunningVersion = () =>
        Axios.get<RunningVersion>(tNoodleBackend + versionEndpoint);

    fetchAvailableFmcTranslations = () =>
        Axios.get<Record<string, string>>(
            tNoodleBackend + fmcTranslationsEndpoint
        );

    fetchZip = (
        scrambleClient: ScrambleClient,
        wcif: Wcif,
        mbld: string,
        password: string,
        status: FrontendStatus,
        translations?: Translation[]
    ) => {
        let payload = {
            wcif,
            multiCubes: { requestedScrambles: mbld },
            fmcLanguages: fmcTranslationsHelper(translations),
            zipPassword: !password ? null : password,
            frontendStatus: status,
        };

        return scrambleClient.loadScrambles(zipEndpoint, payload, wcif.id);
    };

    convertToBlob = async (result: WebsocketBlobResult) => {
        let { contentType, payload } = result;
        let res = await fetch(`data:${contentType};base64,${payload}`);

        return await res.blob();
    };
}

const tnoodleApi = new TnoodleApi();
export default tnoodleApi;

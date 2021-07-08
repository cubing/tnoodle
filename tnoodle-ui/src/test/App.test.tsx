import { fireEvent } from "@testing-library/react";
import { shuffle } from "lodash";
import React from "react";
import { render, unmountComponentAtNode } from "react-dom";
import { act } from "react-dom/test-utils";
import { Provider } from "react-redux";
import App from "../App";
import tnoodleApi from "../main/api/tnoodle.api";
import wcaApi from "../main/api/wca.api";
import Translation from "../main/model/Translation";
import Wcif from "../main/model/Wcif";
import { defaultWcif } from "../main/util/wcif.util";
import {
    bestMbldAttempt,
    defaultStatus,
    events,
    formats,
    languages,
    plainZip,
    version,
} from "./mock/tnoodle.api.test.mock";
import { axiosResponse, getNewStore } from "./mock/util.test.mock";
import {
    competitions,
    me,
    scrambleProgram,
    wcifs,
} from "./mock/wca.api.test.mock";
import FrontendStatus from "../main/model/FrontendStatus";

let container = document.createElement("div");

let wcif: Wcif | null = null;
let mbld: string | null = null;
let password: string | null = null;
let status: FrontendStatus | null = null;
let translations: Translation[] | undefined;
beforeEach(() => {
    // setup a DOM element as a render target
    container = document.createElement("div");
    document.body.appendChild(container);

    // Turn on mocking behavior
    jest.spyOn(tnoodleApi, "fetchWcaEvents").mockImplementation(() =>
        Promise.resolve({
            data: events,
            ...axiosResponse,
        })
    );

    jest.spyOn(tnoodleApi, "fetchFormats").mockImplementation(() =>
        Promise.resolve({ data: formats, ...axiosResponse })
    );

    jest.spyOn(
        tnoodleApi,
        "fetchAvailableFmcTranslations"
    ).mockImplementation(() =>
        Promise.resolve({ data: languages, ...axiosResponse })
    );

    jest.spyOn(tnoodleApi, "fetchRunningVersion").mockImplementation(() =>
        Promise.resolve({ data: version, ...axiosResponse })
    );

    jest.spyOn(tnoodleApi, "fetchZip").mockImplementation(
        (scrambleClient, _wcif, _mbld, _password, _status, _translations) => {
            wcif = _wcif;
            mbld = _mbld;
            password = _password;
            status = _status;
            translations = _translations;

            return Promise.resolve(plainZip);
        }
    );

    jest.spyOn(wcaApi, "fetchVersionInfo").mockImplementation(() =>
        Promise.resolve({ data: scrambleProgram, ...axiosResponse })
    );

    jest.spyOn(
        wcaApi,
        "getUpcomingManageableCompetitions"
    ).mockImplementation(() =>
        Promise.resolve({ ...axiosResponse, data: competitions })
    );

    jest.spyOn(wcaApi, "fetchMe").mockImplementation(() =>
        Promise.resolve({ ...axiosResponse, data: { me } })
    );

    jest.spyOn(tnoodleApi, "convertToBlob").mockImplementation(() =>
        Promise.resolve(new Blob())
    );
});

afterEach(() => {
    // cleanup on exiting
    unmountComponentAtNode(container);
    container.remove();
    container = document.createElement("div");

    wcif = null;
    mbld = null;
    password = null;
    translations = undefined;

    // Clear mock
    jest.spyOn(tnoodleApi, "fetchWcaEvents").mockRestore();
    jest.spyOn(tnoodleApi, "fetchFormats").mockRestore();
    jest.spyOn(tnoodleApi, "fetchAvailableFmcTranslations").mockRestore();
    jest.spyOn(tnoodleApi, "fetchRunningVersion").mockRestore();
    jest.spyOn(tnoodleApi, "fetchZip").mockRestore();
    jest.spyOn(wcaApi, "getUpcomingManageableCompetitions").mockRestore();
    jest.spyOn(tnoodleApi, "convertToBlob").mockRestore();
    jest.spyOn(wcaApi, "fetchMe").mockRestore();
    jest.spyOn(wcaApi, "fetchVersionInfo").mockRestore();
});

it("Just generate scrambles", async () => {
    const store = getNewStore();

    // Render component
    await act(async () => {
        render(
            <React.StrictMode>
                <Provider store={store}>
                    <App />
                </Provider>
            </React.StrictMode>,
            container
        );
    });
    const scrambleButton = container.querySelector("form button")!;
    expect(scrambleButton.innerHTML).toEqual("Generate Scrambles");

    // Generate scrambles
    await act(async () => {
        fireEvent.click(scrambleButton);
    });

    // Only 333
    expect(wcif!.events.length).toBe(1);

    expect(password).toBe("");
    expect(status).toEqual(defaultStatus);
});

it("Changes on 333, scramble", async () => {
    const store = getNewStore();

    // Render component
    await act(async () => {
        render(
            <React.StrictMode>
                <Provider store={store}>
                    <App />
                </Provider>
            </React.StrictMode>,
            container
        );
    });

    let selects = Array.from(container.querySelectorAll("select"));

    // We add rounds of 333
    let numberOfRounds = 4;
    fireEvent.change(selects[0], { target: { value: numberOfRounds } });

    // Look for selects again
    selects = Array.from(container.querySelectorAll("select"));

    // Change 2nd round to mo3
    let roundFormat = "3";
    fireEvent.change(selects[2], { target: { value: roundFormat } });

    let inputs = Array.from(container.querySelectorAll("form input"));

    // Change 2nd round
    let scrambleSets = "6";
    let copies = "7";
    fireEvent.change(inputs[4], { target: { value: scrambleSets } });
    fireEvent.change(inputs[5], { target: { value: copies } });

    // Change password
    let newPassword = "wca123";
    fireEvent.change(inputs[1], { target: { value: newPassword } });

    // Generate scrambles
    const scrambleButton = container.querySelector("form button");
    await act(async () => {
        fireEvent.click(scrambleButton!);
    });

    // Only 333
    expect(wcif!.events.length).toBe(1);

    // Correct number of rounds
    expect(wcif!.events[0].rounds.length).toBe(numberOfRounds);

    // Changes should be done to the 2nd round only
    wcif!.events[0].rounds.forEach((round, i) => {
        if (i === 1) {
            expect(round.format).toBe(roundFormat);
            expect(round.scrambleSetCount).toBe(scrambleSets);
            expect(round.extensions[0].data.numCopies).toBe(copies);
        } else {
            expect(round.format).toBe("a");
            expect(round.scrambleSetCount).toBe("1");
            expect(round.extensions[0].data.numCopies).toBe("1");
        }

        expect(round.id).toBe("333-r" + (i + 1));

        // We only send 1 extension for now
        expect(round.extensions.length).toBe(1);
    });

    expect(password).toBe(newPassword);
});

it("Remove 333, add FMC and MBLD", async () => {
    const store = getNewStore();

    // Render component
    await act(async () => {
        render(
            <React.StrictMode>
                <Provider store={store}>
                    <App />
                </Provider>
            </React.StrictMode>,
            container
        );
    });

    const mbldEvent = "3x3x3 Multiple Blindfolded";
    const fmcEvent = "3x3x3 Fewest Moves";

    let events = Array.from(container.querySelectorAll("form table"));
    const names = [mbldEvent, fmcEvent];

    let mbldCubes = "70";

    // Pick random indexes from fmc to deselect
    let laguageKeys = Object.keys(languages);
    let numberOfLanguages = laguageKeys.length;

    // At least 1, we do not deselect every translation
    let languagesToDeselect =
        Math.floor(Math.random() * numberOfLanguages - 2) + 1;

    let languagesIndexToDelesect = shuffle([
        ...Array.from(Array(numberOfLanguages).keys()),
    ]).slice(languagesToDeselect);

    for (let i = 0; i < events.length; i++) {
        let event = events[i];
        let title = event.querySelector("h5")!.innerHTML;

        let rounds = event.querySelector("select")!;

        if (names.includes(title)) {
            await act(async () => {
                fireEvent.change(rounds, { target: { value: 3 } });
            });
        } else {
            await act(async () => {
                fireEvent.change(rounds, { target: { value: 0 } });
            });
        }

        let inputs = Array.from(event.querySelectorAll("input"));

        // Change to 70 mbld
        if (title === mbldEvent) {
            fireEvent.change(inputs[inputs.length - 1], {
                target: { value: mbldCubes },
            });
        } else if (title === fmcEvent) {
            // Open translations
            await act(async () => {
                fireEvent.click(event.querySelector("button")!);
            });

            // Deselesect random translations
            let checkboxes = Array.from(
                event.querySelectorAll("input[type=checkbox]")
            );

            for (
                let index = 0;
                index < languagesIndexToDelesect.length;
                index++
            ) {
                await act(async () => {
                    fireEvent.click(
                        checkboxes[languagesIndexToDelesect[index]]
                    );
                });
            }
        }
    }

    // Generate scrambles
    const scrambleButton = container.querySelector("form button")!;
    await act(async () => {
        fireEvent.click(scrambleButton);
    });

    expect(wcif!.events.length).toBe(events.length);

    expect(mbld).toBe(mbldCubes);

    let selected = translations!
        .filter((translation) => translation.status)
        .map((translation) => translation.id);

    let deselected = translations!
        .filter((translation) => !translation.status)
        .map((translation) => translation.id)
        .sort();

    // Deselected should be with status false
    expect(deselected).toEqual(
        languagesIndexToDelesect.map((index) => laguageKeys[index]).sort()
    );

    // Selected and deselected should cover every languages
    expect([...selected, ...deselected].sort()).toStrictEqual(
        laguageKeys.sort()
    );
});

it("Logged user", async () => {
    const store = getNewStore();

    // Allow downloads
    global.URL.createObjectURL = jest.fn();

    jest.spyOn(wcaApi, "isLogged").mockImplementation(() => true);

    jest.spyOn(
        wcaApi,
        "getCompetitionJson"
    ).mockImplementation((competitionId) =>
        Promise.resolve({ ...axiosResponse, data: wcifs[competitionId] })
    );

    jest.spyOn(tnoodleApi, "fetchBestMbldAttempt").mockImplementation(() =>
        Promise.resolve({
            ...axiosResponse,
            data: bestMbldAttempt,
        })
    );

    jest.spyOn(
        tnoodleApi,
        "fetchSuggestedFmcTranslations"
    ).mockImplementation(() =>
        Promise.resolve({ ...axiosResponse, data: ["de", "es", "pt-BR"] })
    );

    // Render component
    await act(async () => {
        render(
            <React.StrictMode>
                <Provider store={store}>
                    <App />
                </Provider>
            </React.StrictMode>,
            container
        );
    });

    let competitionButtons = Array.from(
        container.querySelectorAll("ul button")
    );

    let scrambleButton = container.querySelector("form button")!;

    // Skip Manual Selection, click the other buttons
    for (let i = 0; i < competitions.length; i++) {
        // Select current competition
        await act(async () => {
            // +1 to skip manual selection
            competitionButtons[i + 1].dispatchEvent(
                new MouseEvent("click", { bubbles: true })
            );
        });

        await act(async () => {
            scrambleButton.dispatchEvent(
                new MouseEvent("click", { bubbles: true })
            );
        });

        // We should send received wcif to tnoodle
        expect(wcif).toStrictEqual(wcifs[competitions[i].id]);

        // Download
        await act(async () => {
            scrambleButton.dispatchEvent(
                new MouseEvent("click", { bubbles: true })
            );
        });

        // We should warn in case of mbld
        if (
            !!store
                .getState()
                .wcifSlice.wcif.events.find((event) => event.id === "333mbf") &&
            store.getState().mbldSlice.bestMbldAttempt! >
                Number(store.getState().mbldSlice.mbld)
        ) {
            let items = container.querySelectorAll("tfoot tr th[colspan]");
            expect(items[items.length - 1].innerHTML).toContain(
                `a competitor who already tried ${bestMbldAttempt.attempted} at a competition. Proceed if you are really certain of it.`
            );
        }
    }

    // Get back to manual selection
    await act(async () => {
        competitionButtons[0].dispatchEvent(
            new MouseEvent("click", { bubbles: true })
        );
    });

    await act(async () => {
        scrambleButton.dispatchEvent(
            new MouseEvent("click", { bubbles: true })
        );
    });

    // After manual selection, events should be restored
    expect(store.getState().wcifSlice.wcif.events).toStrictEqual(
        defaultWcif.events
    );

    // Wcifs should be cached
    Object.keys(wcifs).forEach((competitionId) => {
        expect(
            store.getState().informationSlice.cachedObjects[competitionId].wcif
        ).toEqual(wcifs[competitionId]);
    });

    // Click all buttons again
    for (let i = 0; i < competitionButtons.length; i++) {
        await act(async () => {
            competitionButtons[i].dispatchEvent(
                new MouseEvent("click", { bubbles: true })
            );
        });
    }

    // On the 2nd competition selection, we should use cached information,
    // so no wcif should be called
    expect(wcaApi.getCompetitionJson).toHaveBeenCalledTimes(
        competitions.length
    );

    jest.spyOn(global.URL, "createObjectURL").mockRestore();
    jest.spyOn(wcaApi, "isLogged").mockRestore();
    jest.spyOn(wcaApi, "getCompetitionJson").mockRestore();
    jest.spyOn(tnoodleApi, "fetchBestMbldAttempt").mockRestore();
    jest.spyOn(tnoodleApi, "fetchSuggestedFmcTranslations").mockRestore();
});

it("Comfort features should not block zip generation", async () => {
    const store = getNewStore();

    // Allow downloads
    global.URL.createObjectURL = jest.fn();

    jest.spyOn(wcaApi, "isLogged").mockImplementation(() => true);

    jest.spyOn(
        wcaApi,
        "getCompetitionJson"
    ).mockImplementation((competitionId) =>
        Promise.resolve({ ...axiosResponse, data: wcifs[competitionId] })
    );

    // Comfort features
    jest.spyOn(tnoodleApi, "fetchBestMbldAttempt").mockImplementation(() =>
        Promise.resolve({ ...axiosResponse, data: bestMbldAttempt })
    );

    jest.spyOn(
        tnoodleApi,
        "fetchSuggestedFmcTranslations"
    ).mockImplementation(() => Promise.resolve({ ...axiosResponse, data: [] }));

    // Render component
    await act(async () => {
        render(
            <React.StrictMode>
                <Provider store={store}>
                    <App />
                </Provider>
            </React.StrictMode>,
            container
        );
    });

    let competitionButtons = Array.from(
        container.querySelectorAll("ul button")
    );

    let scrambleButton = container.querySelector("form button")!;

    // Click competitions
    for (let i = 0; i < competitionButtons.length; i++) {
        await act(async () => {
            competitionButtons[i].dispatchEvent(
                new MouseEvent("click", { bubbles: true })
            );
        });

        // Round changes from previous tests also changes defaultWcif
        // to avoid empty rounds, we try to change rounds here
        // It should have effect just on in Manual Selection
        await act(async () => {
            fireEvent.change(container.querySelector("select")!, {
                target: { value: 1 },
            });
        });

        await act(async () => {
            scrambleButton.dispatchEvent(
                new MouseEvent("click", { bubbles: true })
            );
        });

        await act(async () => {
            scrambleButton.dispatchEvent(
                new MouseEvent("click", { bubbles: true })
            );
        });
    }

    expect(tnoodleApi.fetchZip).toHaveBeenCalledTimes(
        competitionButtons.length
    );

    jest.spyOn(global.URL, "createObjectURL").mockRestore();
    jest.spyOn(wcaApi, "isLogged").mockRestore();
    jest.spyOn(wcaApi, "getCompetitionJson").mockRestore();
    jest.spyOn(tnoodleApi, "fetchBestMbldAttempt").mockRestore();
    jest.spyOn(tnoodleApi, "fetchSuggestedFmcTranslations").mockRestore();
});

"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
const commandLineArgs = require("command-line-args");
const DEFAULT_GLOB_PATTERN = "**/*.abi";
function parseArgs() {
    const optionDefinitions = [
        { name: "force", alias: "f", type: Boolean },
        { name: "glob", type: String, defaultOption: true },
        { name: "outDir", type: String },
    ];
    const rawOptions = commandLineArgs(optionDefinitions);
    return {
        force: !!rawOptions.force,
        glob: rawOptions.glob || DEFAULT_GLOB_PATTERN,
        outDir: rawOptions.outDir,
    };
}
exports.parseArgs = parseArgs;

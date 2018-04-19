"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
function getVersion() {
    const packageJson = require("../package.json");
    return packageJson.version;
}
exports.getVersion = getVersion;

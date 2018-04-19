"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
const fs_extra_1 = require("fs-extra");
const path_1 = require("path");
function copyRuntime(path) {
    fs_extra_1.copySync(path_1.join(__dirname, "../runtime/typechain-runtime.ts"), path);
}
exports.copyRuntime = copyRuntime;

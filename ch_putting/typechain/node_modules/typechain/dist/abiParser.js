"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
const debug_1 = require("./debug");
const typeParser_1 = require("./typeParser");
const chalk_1 = require("chalk");
const errors_1 = require("./errors");
const { yellow } = chalk_1.default;
function parse(abi) {
    const constants = [];
    const constantFunctions = [];
    const functions = [];
    abi.forEach(abiPiece => {
        // @todo implement missing abi pieces
        // skip constructors for now
        if (abiPiece.type === "constructor") {
            return;
        }
        // skip events
        if (abiPiece.type === "event") {
            return;
        }
        // skip fallback functions
        if (abiPiece.type === "fallback") {
            return;
        }
        if (abiPiece.type === "function") {
            if (checkForOverloads(constants, constantFunctions, functions, abiPiece.name)) {
                // tslint:disable-next-line
                console.log(yellow(`Detected overloaded constant function ${abiPiece.name} skipping...`));
                return;
            }
            if (abiPiece.constant && abiPiece.inputs.length === 0 && abiPiece.outputs.length === 1) {
                constants.push(parseConstant(abiPiece));
            }
            else if (abiPiece.constant) {
                constantFunctions.push(parseConstantFunction(abiPiece));
            }
            else {
                functions.push(parseFunctionDeclaration(abiPiece));
            }
            return;
        }
        throw new Error(`Unrecognized abi element: ${abiPiece.type}`);
    });
    return {
        constants,
        constantFunctions,
        functions,
    };
}
exports.parse = parse;
function checkForOverloads(constants, constantFunctions, functions, name) {
    return (constantFunctions.find(f => f.name === name) ||
        constants.find(f => f.name === name) ||
        functions.find(f => f.name === name));
}
function parseOutputs(outputs) {
    if (outputs.length === 0) {
        return [new typeParser_1.VoidType()];
    }
    else {
        return outputs.map(param => typeParser_1.parseEvmType(param.type));
    }
}
function parseConstant(abiPiece) {
    debug_1.default(`Parsing constant "${abiPiece.name}"`);
    return {
        name: abiPiece.name,
        output: typeParser_1.parseEvmType(abiPiece.outputs[0].type),
    };
}
function parseConstantFunction(abiPiece) {
    debug_1.default(`Parsing constant function "${abiPiece.name}"`);
    return {
        name: abiPiece.name,
        inputs: abiPiece.inputs.map(parseRawAbiParameter),
        outputs: parseOutputs(abiPiece.outputs),
    };
}
function parseFunctionDeclaration(abiPiece) {
    debug_1.default(`Parsing function declaration "${abiPiece.name}"`);
    return {
        name: abiPiece.name,
        inputs: abiPiece.inputs.map(parseRawAbiParameter),
        outputs: parseOutputs(abiPiece.outputs),
        payable: abiPiece.payable,
    };
}
function parseRawAbiParameter(rawAbiParameter) {
    return {
        name: rawAbiParameter.name,
        type: typeParser_1.parseEvmType(rawAbiParameter.type),
    };
}
function extractAbi(rawJson) {
    let json;
    try {
        json = JSON.parse(rawJson);
    }
    catch (_a) {
        throw new errors_1.MalformedAbiError("Not a json");
    }
    if (!json) {
        throw new errors_1.MalformedAbiError("Not a json");
    }
    if (Array.isArray(json)) {
        return json;
    }
    if (Array.isArray(json.abi)) {
        return json.abi;
    }
    throw new errors_1.MalformedAbiError("Not a valid ABI");
}
exports.extractAbi = extractAbi;

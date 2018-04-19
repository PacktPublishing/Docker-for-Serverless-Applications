import { EvmType } from "./typeParser";
export interface AbiParameter {
    name: string;
    type: EvmType;
}
export interface ConstantDeclaration {
    name: string;
    output: EvmType;
}
export interface ConstantFunctionDeclaration {
    name: string;
    inputs: Array<AbiParameter>;
    outputs: Array<EvmType>;
}
export interface FunctionDeclaration {
    name: string;
    inputs: Array<AbiParameter>;
    outputs: Array<EvmType>;
    payable: boolean;
}
export interface Contract {
    constants: Array<ConstantDeclaration>;
    constantFunctions: Array<ConstantFunctionDeclaration>;
    functions: Array<FunctionDeclaration>;
}
export interface RawAbiParameter {
    name: string;
    type: string;
}
export interface RawAbiDefinition {
    name: string;
    constant: boolean;
    payable: boolean;
    inputs: RawAbiParameter[];
    outputs: RawAbiParameter[];
    type: string;
}
export declare function parse(abi: Array<RawAbiDefinition>): Contract;
export declare function extractAbi(rawJson: string): RawAbiDefinition[];

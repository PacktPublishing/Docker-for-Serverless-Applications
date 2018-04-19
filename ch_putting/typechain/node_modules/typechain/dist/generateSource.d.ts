import { RawAbiDefinition } from "./abiParser";
export interface IContext {
    fileName: string;
    relativeRuntimePath: string;
}
export declare function generateSource(abi: Array<RawAbiDefinition>, context: IContext): string;

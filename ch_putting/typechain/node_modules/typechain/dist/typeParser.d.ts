export declare abstract class EvmType {
    generateCodeForInput(): string;
    abstract generateCodeForOutput(): string;
}
export declare class BooleanType extends EvmType {
    generateCodeForOutput(): string;
}
export declare class IntegerType extends EvmType {
    readonly bits: number;
    constructor(bits: number);
    generateCodeForInput(): string;
    generateCodeForOutput(): string;
}
export declare class UnsignedIntegerType extends EvmType {
    readonly bits: number;
    constructor(bits: number);
    generateCodeForInput(): string;
    generateCodeForOutput(): string;
}
export declare class VoidType extends EvmType {
    generateCodeForOutput(): string;
}
export declare class StringType extends EvmType {
    generateCodeForOutput(): string;
}
export declare class BytesType extends EvmType {
    readonly size: number;
    constructor(size: number);
    generateCodeForOutput(): string;
}
export declare class AddressType extends EvmType {
    generateCodeForOutput(): string;
    generateCodeForInput(): string;
}
export declare class ArrayType extends EvmType {
    readonly itemType: EvmType;
    readonly size: number | undefined;
    constructor(itemType: EvmType, size?: number | undefined);
    generateCodeForOutput(): string;
}
export declare function parseEvmType(rawType: string): EvmType;

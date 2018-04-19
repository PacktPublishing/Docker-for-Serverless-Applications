export interface IOptions {
    glob: string;
    force: boolean;
    outDir?: string;
}
export declare function parseArgs(): IOptions;

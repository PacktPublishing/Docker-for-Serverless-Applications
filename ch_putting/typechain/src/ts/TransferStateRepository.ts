/* GENERATED BY TYPECHAIN VER. 0.1.2 */
/* tslint:disable */

import { BigNumber } from "bignumber.js";
import {
  TypeChainContract,
  promisify,
  ITxParams,
  IPayableTxParams,
  DeferredTransactionWrapper
} from "./typechain-runtime";

export class TransferStateRepository extends TypeChainContract {
  public readonly rawWeb3Contract: any;

  public constructor(web3: any, address: string | BigNumber) {
    const abi = [
      {
        constant: false,
        inputs: [{ name: "txId", type: "string" }],
        name: "start",
        outputs: [],
        payable: false,
        type: "function"
      },
      {
        constant: true,
        inputs: [{ name: "txId", type: "string" }],
        name: "getStateOf",
        outputs: [{ name: "", type: "string" }],
        payable: false,
        type: "function"
      },
      {
        constant: false,
        inputs: [{ name: "txId", type: "string" }],
        name: "complete",
        outputs: [],
        payable: false,
        type: "function"
      },
      {
        constant: false,
        inputs: [{ name: "txId", type: "string" }],
        name: "pending",
        outputs: [],
        payable: false,
        type: "function"
      },
      {
        anonymous: false,
        inputs: [{ indexed: false, name: "txId", type: "string" }],
        name: "TransferStarted",
        type: "event"
      },
      {
        anonymous: false,
        inputs: [{ indexed: false, name: "txId", type: "string" }],
        name: "TransferPending",
        type: "event"
      },
      {
        anonymous: false,
        inputs: [{ indexed: false, name: "txId", type: "string" }],
        name: "TransferCompleted",
        type: "event"
      }
    ];
    super(web3, address, abi);
  }

  static async createAndValidate(
    web3: any,
    address: string | BigNumber
  ): Promise<TransferStateRepository> {
    const contract = new TransferStateRepository(web3, address);
    const code = await promisify(web3.eth.getCode, [address]);
    if (code === "0x0") {
      throw new Error(`Contract at ${address} doesn't exist!`);
    }
    return contract;
  }

  public getStateOf(txId: string): Promise<string> {
    return promisify(this.rawWeb3Contract.getStateOf, [txId.toString()]);
  }

  public startTx(txId: string): DeferredTransactionWrapper<ITxParams> {
    return new DeferredTransactionWrapper<ITxParams>(this, "start", [
      txId.toString()
    ]);
  }
  public completeTx(txId: string): DeferredTransactionWrapper<ITxParams> {
    return new DeferredTransactionWrapper<ITxParams>(this, "complete", [
      txId.toString()
    ]);
  }
  public pendingTx(txId: string): DeferredTransactionWrapper<ITxParams> {
    return new DeferredTransactionWrapper<ITxParams>(this, "pending", [
      txId.toString()
    ]);
  }
}

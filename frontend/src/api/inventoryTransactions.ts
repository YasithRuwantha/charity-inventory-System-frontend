import { httpClient } from "@/api/client";
import { buildQuery, unwrapPaged } from "@/api/helpers";
import type { Paged } from "@/api/types";
import type { InventoryTransaction } from "@/api/inventory";
import type { ReferenceType, TransactionType } from "@/types/enums";

export interface TransactionLedgerParams {
  itemId?: number;
  transactionType?: TransactionType;
  referenceType?: ReferenceType;
  performedBy?: number;
  startDate?: string;
  endDate?: string;
  page?: number;
  size?: number;
  sort?: string;
}

export async function listAllTransactions(params: TransactionLedgerParams = {}): Promise<Paged<InventoryTransaction>> {
  const res = await httpClient.get(`/api/inventory-transactions${buildQuery(params)}`);
  return unwrapPaged<InventoryTransaction>(res);
}

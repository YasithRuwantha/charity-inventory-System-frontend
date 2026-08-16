export const RECORD_STATUS = ["ACTIVE", "INACTIVE"] as const;
export type RecordStatus = (typeof RECORD_STATUS)[number];

export const USER_ROLES = ["ADMIN", "INVENTORY_STAFF", "VOLUNTEER"] as const;
export type UserRole = (typeof USER_ROLES)[number];

export const USER_STATUS = ["ACTIVE", "INACTIVE"] as const;
export type UserStatus = (typeof USER_STATUS)[number];

export const INVENTORY_STATUS = ["IN_STOCK", "LOW_STOCK", "OUT_OF_STOCK", "EXPIRED"] as const;
export type InventoryStatus = (typeof INVENTORY_STATUS)[number];

export const EXPIRY_FILTER = ["EXPIRED", "EXPIRING_7_DAYS", "EXPIRING_30_DAYS", "NO_EXPIRY"] as const;
export type ExpiryFilter = (typeof EXPIRY_FILTER)[number];

export const UNIT_OF_MEASURE = ["ITEM", "PACK", "BOX", "BOTTLE", "KG", "GRAM", "LITER", "OTHER"] as const;
export type UnitOfMeasure = (typeof UNIT_OF_MEASURE)[number];

export const TRANSACTION_TYPE = ["DONATION_IN", "DISTRIBUTION_OUT", "MANUAL_ADJUSTMENT", "CORRECTION"] as const;
export type TransactionType = (typeof TRANSACTION_TYPE)[number];

export const REFERENCE_TYPE = ["DONATION", "DISTRIBUTION", "MANUAL", "SYSTEM"] as const;
export type ReferenceType = (typeof REFERENCE_TYPE)[number];

export const DONATION_STATUS = ["RECEIVED", "CANCELLED"] as const;
export type DonationStatus = (typeof DONATION_STATUS)[number];

export const DONOR_TYPE = ["INDIVIDUAL", "ORGANIZATION", "ANONYMOUS"] as const;
export type DonorType = (typeof DONOR_TYPE)[number];

export const PRIORITY_LEVEL = ["HIGH", "MEDIUM", "LOW"] as const;
export type PriorityLevel = (typeof PRIORITY_LEVEL)[number];

export const DISTRIBUTION_STATUS = ["PENDING", "APPROVED", "REJECTED", "COMPLETED", "CANCELLED"] as const;
export type DistributionStatus = (typeof DISTRIBUTION_STATUS)[number];

export const TASK_STATUS = ["PENDING", "IN_PROGRESS", "COMPLETED", "CANCELLED"] as const;
export type TaskStatus = (typeof TASK_STATUS)[number];

export const AUDIT_ACTIONS = [
  "CREATE_CATEGORY", "UPDATE_CATEGORY", "CHANGE_CATEGORY_STATUS",
  "CREATE_INVENTORY", "UPDATE_INVENTORY", "ARCHIVE_INVENTORY", "ADJUST_STOCK",
  "CREATE_DONOR", "UPDATE_DONOR", "CHANGE_DONOR_STATUS",
  "REGISTER_DONATION", "BULK_IMPORT_DONATIONS",
  "CREATE_BENEFICIARY", "UPDATE_BENEFICIARY", "CHANGE_BENEFICIARY_STATUS",
  "CREATE_DISTRIBUTION", "UPDATE_DISTRIBUTION", "ALLOCATE_DISTRIBUTION", "APPROVE_DISTRIBUTION",
  "REJECT_DISTRIBUTION", "CANCEL_DISTRIBUTION", "COMPLETE_DISTRIBUTION", "OVERRIDE_DUPLICATE",
  "CREATE_VOLUNTEER", "UPDATE_VOLUNTEER", "CHANGE_VOLUNTEER_STATUS",
  "ASSIGN_VOLUNTEER_TASK", "UPDATE_VOLUNTEER_TASK", "CHANGE_VOLUNTEER_TASK_STATUS",
  "CHANGE_USER_ROLE", "USER_STATUS_CHANGE", "EXPORT_REPORT",
] as const;
export type AuditAction = (typeof AUDIT_ACTIONS)[number];

export const UNIT_LABELS: Record<UnitOfMeasure, string> = {
  ITEM: "Item(s)",
  PACK: "Pack(s)",
  BOX: "Box(es)",
  BOTTLE: "Bottle(s)",
  KG: "Kilogram(s)",
  GRAM: "Gram(s)",
  LITER: "Liter(s)",
  OTHER: "Other",
};

export const PRIORITY_LABELS: Record<PriorityLevel, string> = {
  HIGH: "High",
  MEDIUM: "Medium",
  LOW: "Low",
};

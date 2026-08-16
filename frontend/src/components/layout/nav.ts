import type {
  LucideIcon,
} from "lucide-react";
import {
  LayoutDashboard,
  Package,
  Tags,
  Gift,
  Users,
  HandHeart,
  HardHat,
  BarChart3,
  ShieldCheck,
  UserCog,
  ArrowLeftRight,
} from "lucide-react";
import type { Role } from "@/store/auth";

export interface NavItem {
  label: string;
  to: string;
  icon: LucideIcon;
  roles: Role[];
}

export interface NavSection {
  label?: string;
  items: NavItem[];
}

export const navSections: NavSection[] = [
  {
    items: [
      { label: "Dashboard", to: "/", icon: LayoutDashboard, roles: ["ADMIN", "INVENTORY_STAFF"] },
    ],
  },
  {
    label: "Inventory",
    items: [
      { label: "Items", to: "/inventory", icon: Package, roles: ["ADMIN", "INVENTORY_STAFF", "VOLUNTEER"] },
      { label: "Categories", to: "/inventory/categories", icon: Tags, roles: ["ADMIN", "INVENTORY_STAFF", "VOLUNTEER"] },
      { label: "Stock ledger", to: "/inventory/transactions", icon: ArrowLeftRight, roles: ["ADMIN", "INVENTORY_STAFF"] },
    ],
  },
  {
    label: "Operations",
    items: [
      { label: "Donors", to: "/donors", icon: Gift, roles: ["ADMIN", "INVENTORY_STAFF"] },
      { label: "Donations", to: "/donations", icon: Gift, roles: ["ADMIN", "INVENTORY_STAFF"] },
      { label: "Beneficiaries", to: "/beneficiaries", icon: Users, roles: ["ADMIN", "INVENTORY_STAFF"] },
      { label: "Distributions", to: "/distributions", icon: HandHeart, roles: ["ADMIN", "INVENTORY_STAFF"] },
      { label: "Volunteers", to: "/volunteers", icon: HardHat, roles: ["ADMIN", "INVENTORY_STAFF"] },
      { label: "My tasks", to: "/my-tasks", icon: HardHat, roles: ["VOLUNTEER"] },
    ],
  },
  {
    label: "Insights",
    items: [
      { label: "Reports", to: "/reports", icon: BarChart3, roles: ["ADMIN", "INVENTORY_STAFF"] },
    ],
  },
  {
    label: "Administration",
    items: [
      { label: "Users", to: "/admin/users", icon: UserCog, roles: ["ADMIN"] },
      { label: "Audit log", to: "/admin/audit-log", icon: ShieldCheck, roles: ["ADMIN"] },
    ],
  },
];

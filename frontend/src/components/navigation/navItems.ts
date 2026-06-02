export type NavVisibility = "all" | "member" | "production" | "admin";

export type NavItem = {
  label: string;
  path: string;
  visibility: NavVisibility;
};

export const navItems: NavItem[] = [
  {
    label: "Home",
    path: "/dashboard",
    visibility: "all",
  },
  {
    label: "Events",
    path: "/events",
    visibility: "all",
  },
  {
    label: "Active Order",
    path: "/orders/active",
    visibility: "all",
  },
  {
    label: "Order History",
    path: "/orders/history",
    visibility: "member",
  },
  {
    label: "Notifications",
    path: "/notifications",
    visibility: "member",
  },
  {
    label: "My Companies",
    path: "/production-company",
    visibility: "member",
  },
  {
    label: "Purchase Policies",
    path: "/policies",
    visibility: "production",
  },
  {
    label: "Admin",
    path: "/admin",
    visibility: "admin",
  },
];
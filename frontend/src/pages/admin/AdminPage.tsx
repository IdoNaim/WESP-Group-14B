import { useEffect, useMemo, useState } from "react";
import { adminApi, type SystemHistoryOrderDTO, type SystemActiveOrderDTO, type SystemUserDTO } from "../../api/adminApi";
import { useAuth } from "../../context/AuthContext";
import "./AdminPage.scss";

type AdminTab = "overview" | "users" | "activeOrders" | "historyOrders";

type AdminDataState = {
  users: SystemUserDTO[];
  activeOrders: SystemActiveOrderDTO[];
  historyOrders: SystemHistoryOrderDTO[];
};

export default function AdminPage() {
  const { token, isAdmin } = useAuth();

  const [selectedTab, setSelectedTab] = useState<AdminTab>("overview");
  const [data, setData] = useState<AdminDataState>({
    users: [],
    activeOrders: [],
    historyOrders: [],
  });

  const [loading, setLoading] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  const totalRevenue = useMemo(() => {
    return data.historyOrders.reduce((sum, order) => {
      const price = typeof order.price === "number" ? order.price : 0;
      return sum + price;
    }, 0);
  }, [data.historyOrders]);

  useEffect(() => {
    if (!token || !isAdmin) {
      return;
    }

    const loadAdminData = async () => {
      setLoading(true);
      setErrorMessage(null);

      try {
        const [users, activeOrders, historyOrders] = await Promise.all([
          adminApi.getSystemUsers(token),
          adminApi.getActiveOrders(token),
          adminApi.getOrderHistory(token),
        ]);

        setData({
          users,
          activeOrders,
          historyOrders,
        });
      } catch (error: any) {
        setErrorMessage(error.message || "Failed to load admin data.");
      } finally {
        setLoading(false);
      }
    };

    loadAdminData();
  }, [token, isAdmin]);

  return (
    <section className="admin-page">
      <header className="admin-page__header">
        <div>
          <p className="admin-page__eyebrow">System Administration</p>
          <h1 className="admin-page__title">Admin Panel</h1>
          <p className="admin-page__subtitle">
            Monitor users, active orders, and global order history.
          </p>
        </div>
      </header>

      {errorMessage && (
        <div className="admin-page__error">
          {errorMessage}
        </div>
      )}

      <div className="admin-page__stats-grid">
        <button
          type="button"
          className="admin-page__stat-card"
          onClick={() => setSelectedTab("users")}
        >
          <span className="admin-page__stat-label">Users</span>
          <strong className="admin-page__stat-value">{data.users.length}</strong>
        </button>

        <button
          type="button"
          className="admin-page__stat-card"
          onClick={() => setSelectedTab("activeOrders")}
        >
          <span className="admin-page__stat-label">Active Orders</span>
          <strong className="admin-page__stat-value">
            {data.activeOrders.length}
          </strong>
        </button>

        <button
          type="button"
          className="admin-page__stat-card"
          onClick={() => setSelectedTab("historyOrders")}
        >
          <span className="admin-page__stat-label">History Orders</span>
          <strong className="admin-page__stat-value">
            {data.historyOrders.length}
          </strong>
        </button>

        <div className="admin-page__stat-card admin-page__stat-card--static">
          <span className="admin-page__stat-label">Recorded Revenue</span>
          <strong className="admin-page__stat-value">
            ₪{totalRevenue.toFixed(2)}
          </strong>
        </div>
      </div>

      <div className="admin-page__tabs">
        <button
          type="button"
          className={selectedTab === "overview" ? "admin-page__tab admin-page__tab--active" : "admin-page__tab"}
          onClick={() => setSelectedTab("overview")}
        >
          Overview
        </button>

        <button
          type="button"
          className={selectedTab === "users" ? "admin-page__tab admin-page__tab--active" : "admin-page__tab"}
          onClick={() => setSelectedTab("users")}
        >
          Users
        </button>

        <button
          type="button"
          className={selectedTab === "activeOrders" ? "admin-page__tab admin-page__tab--active" : "admin-page__tab"}
          onClick={() => setSelectedTab("activeOrders")}
        >
          Active Orders
        </button>

        <button
          type="button"
          className={selectedTab === "historyOrders" ? "admin-page__tab admin-page__tab--active" : "admin-page__tab"}
          onClick={() => setSelectedTab("historyOrders")}
        >
          History Orders
        </button>
      </div>

      <div className="admin-page__panel">
        {loading && <p className="admin-page__muted">Loading admin data...</p>}

        {!loading && selectedTab === "overview" && (
          <AdminOverview />
        )}

        {!loading && selectedTab === "users" && (
          <AdminUsersTable users={data.users} />
        )}

        {!loading && selectedTab === "activeOrders" && (
          <AdminGenericTable
            title="Active Orders"
            emptyMessage="No active orders found."
            items={data.activeOrders}
          />
        )}

        {!loading && selectedTab === "historyOrders" && (
          <AdminGenericTable
            title="History Orders"
            emptyMessage="No history orders found."
            items={data.historyOrders}
          />
        )}
      </div>
    </section>
  );
}

function AdminOverview() {
  return (
    <div className="admin-page__overview">
      <h2>Available Admin Actions</h2>

      <div className="admin-page__action-grid">
        <div className="admin-page__action-card">
          <h3>User Management</h3>
          <p>View registered users and prepare future account-management actions.</p>
        </div>

        <div className="admin-page__action-card">
          <h3>Active Orders Monitoring</h3>
          <p>Review active orders currently open in the system.</p>
        </div>

        <div className="admin-page__action-card">
          <h3>Global Order History</h3>
          <p>View completed purchases across the platform.</p>
        </div>

        <div className="admin-page__action-card admin-page__action-card--disabled">
          <h3>Company / Complaint Tools</h3>
          <p>Reserved for future backend support.</p>
        </div>
      </div>
    </div>
  );
}

function AdminUsersTable({ users }: { users: SystemUserDTO[] }) {
  if (users.length === 0) {
    return <p className="admin-page__muted">No users found.</p>;
  }

  return (
    <div>
      <h2>Users</h2>

      <div className="admin-page__table-wrapper">
        <table className="admin-page__table">
          <thead>
            <tr>
              <th>User ID</th>
              <th>Name</th>
              <th>Email</th>
              <th>State</th>
              <th>Is Admin</th>
            </tr>
          </thead>

          <tbody>
            {users.map((user, index) => (
              <tr key={String(user.id ?? user.userId ?? index)}>
                <td>{String(user.id ?? user.userId ?? "—")}</td>
                <td>{String(user.username ?? user.name ?? "—")}</td>
                <td>{String(user.email ?? "—")}</td>
                <td>{String(user.userState ?? "—")}</td>
                <td>{user.isAdmin === true ? "Yes" : user.isAdmin === false ? "No" : "—"}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}

function AdminGenericTable({
  title,
  emptyMessage,
  items,
}: {
  title: string;
  emptyMessage: string;
  items: Array<Record<string, unknown>>;
}) {
  if (items.length === 0) {
    return <p className="admin-page__muted">{emptyMessage}</p>;
  }

  const columns = Array.from(
    items.reduce((keys, item) => {
      Object.keys(item).forEach((key) => keys.add(key));
      return keys;
    }, new Set<string>())
  ).slice(0, 8);

  return (
    <div>
      <h2>{title}</h2>

      <div className="admin-page__table-wrapper">
        <table className="admin-page__table">
          <thead>
            <tr>
              {columns.map((column) => (
                <th key={column}>{column}</th>
              ))}
            </tr>
          </thead>

          <tbody>
            {items.map((item, index) => (
              <tr key={String(item.orderId ?? index)}>
                {columns.map((column) => (
                  <td key={column}>{formatCellValue(item[column])}</td>
                ))}
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}

function formatCellValue(value: unknown): string {
  if (value === null || value === undefined) {
    return "—";
  }

  if (Array.isArray(value)) {
    return value.join(", ");
  }

  if (typeof value === "object") {
    return JSON.stringify(value);
  }

  return String(value);
}
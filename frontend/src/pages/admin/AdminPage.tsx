import { useEffect, useMemo, useState } from "react";
import { adminApi, type SystemHistoryOrderDTO, type SystemActiveOrderDTO, type SystemUserDTO } from "../../api/adminApi";
import { historyOrderApi, type HistoryOrderDTO } from "../../api/historyOrderApi";
import { useAuth } from "../../context/AuthContext";
import "./AdminPage.scss";

type AdminTab = "overview" | "users" | "activeOrders" | "historyOrders";

type AdminDataState = {
  users: SystemUserDTO[];
  activeOrders: SystemActiveOrderDTO[];
  historyOrders: SystemHistoryOrderDTO[];
};

export default function AdminPage() {
  const { token, isAdmin, loading: authLoading } = useAuth();

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
    if (authLoading) return;

    if (!token || !isAdmin) {
      setErrorMessage("Access denied: admin privileges required.");
      return;
    }

    const loadAdminData = async () => {
      setLoading(true);
      setErrorMessage(null);

      try {
        const [users, activeOrders, historyOrders] = await Promise.all([
          adminApi.getSystemUsers(token).catch(e => { throw new Error(`Users: ${e.message}`); }),
          adminApi.getActiveOrders(token).catch(e => { throw new Error(`Active orders: ${e.message}`); }),
          adminApi.getOrderHistory(token).catch(e => { throw new Error(`History orders: ${e.message}`); }),
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
  }, [token, isAdmin, authLoading]);

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

      {!errorMessage && (
        <>
          <div className="admin-page__stats-grid">
            <button
              type="button"
              className="admin-page__stat-card"
              onClick={() => setSelectedTab("users")}
              disabled={loading}
            >
              <span className="admin-page__stat-label">Users</span>
              <strong className="admin-page__stat-value">{data.users.length}</strong>
            </button>

            <button
              type="button"
              className="admin-page__stat-card"
              onClick={() => setSelectedTab("activeOrders")}
              disabled={loading}
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
              disabled={loading}
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
              <AdminHistorySearch token={token!} />
            )}
          </div>
        </>
      )}
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
          <h3>History Orders Search</h3>
          <p>Look up completed purchases by user ID or company ID.</p>
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
{/* <<<<<<< HEAD */}
                <td>{String(user.userState ?? "—")}</td>
                <td>{user.isAdmin === true ? "Yes" : user.isAdmin === false ? "No" : "—"}</td>
{/* // =======
//                 <td>{String(user.userGroupDiscount ?? "—")}</td>
// >>>>>>> origin/main */}
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

  const allColumns = Array.from(
    items.reduce((keys, item) => {
      Object.keys(item).forEach((key) => keys.add(key));
      return keys;
    }, new Set<string>())
  );
  const truncated = allColumns.length > 8;
  const columns = allColumns.slice(0, 8);

  return (
    <div>
      <h2>{title}</h2>
      {truncated && (
        <p className="admin-page__muted">
          Showing 8 of {allColumns.length} columns.
        </p>
      )}

      <div className="admin-page__table-wrapper">
        <table className="admin-page__table">
          <thead>
            <tr>
              {columns.map((column) => (
                <th key={column}>{formatColumnLabel(column)}</th>
              ))}
            </tr>
          </thead>

          <tbody>
            {items.map((item, index) => (
              <tr key={String(item.orderId ?? item.id ?? index)}>
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

function formatColumnLabel(key: string): string {
  return key
    .replace(/([A-Z])/g, " $1")
    .replace(/^./, (c) => c.toUpperCase())
    .trim();
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

type HistorySearchMode = "user" | "company";

function AdminHistorySearch({ token }: { token: string }) {
  const [mode, setMode] = useState<HistorySearchMode>("user");
  const [inputValue, setInputValue] = useState("");
  const [results, setResults] = useState<HistoryOrderDTO[]>([]);
  const [searched, setSearched] = useState(false);
  const [searching, setSearching] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleModeChange = (newMode: HistorySearchMode) => {
    setMode(newMode);
    setInputValue("");
    setResults([]);
    setSearched(false);
    setError(null);
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!inputValue.trim()) return;
    setSearching(true);
    setError(null);
    try {
      let data: HistoryOrderDTO[];
      if (mode === "user") {
        data = await historyOrderApi.getUserOrders(token, inputValue.trim());
      } else {
        const id = parseInt(inputValue, 10);
        if (isNaN(id)) { setError("Company ID must be a number."); return; }
        data = await historyOrderApi.getOrdersByCompany(token, id);
      }
      setResults(data);
      setSearched(true);
    } catch (err: any) {
      setError(err.message || "Failed to fetch orders.");
    } finally {
      setSearching(false);
    }
  };

  const inputStyle: React.CSSProperties = {
    flex: 1,
    padding: "0.65rem 1rem",
    borderRadius: "0.5rem",
    border: "1px solid rgba(15,23,42,0.2)",
    fontSize: "0.9rem",
  };

  const selectStyle: React.CSSProperties = {
    padding: "0.65rem 1rem",
    borderRadius: "0.5rem",
    border: "1px solid rgba(15,23,42,0.2)",
    fontSize: "0.9rem",
    background: "white",
    cursor: "pointer",
  };

  return (
    <div>
      <h2>History Orders</h2>
      <form onSubmit={handleSubmit} style={{ display: "flex", gap: "0.75rem", marginBottom: "1.5rem", alignItems: "center" }}>
        <select
          value={mode}
          onChange={(e) => handleModeChange(e.target.value as HistorySearchMode)}
          style={selectStyle}
        >
          <option value="user">By User</option>
          <option value="company">By Company</option>
        </select>
        <input
          type={mode === "company" ? "number" : "text"}
          value={inputValue}
          onChange={(e) => setInputValue(e.target.value)}
          placeholder={mode === "user" ? "Enter user ID" : "Enter company ID"}
          style={inputStyle}
        />
        <button
          type="submit"
          disabled={searching || !inputValue.trim()}
          className="admin-page__tab admin-page__tab--active"
        >
          {searching ? "Searching…" : "Search"}
        </button>
      </form>
      {error && <div className="admin-page__error">{error}</div>}
      {!error && searched && (
        <AdminGenericTable
          title=""
          emptyMessage={mode === "user" ? "No orders found for this user." : "No orders found for this company."}
          items={results as unknown as Array<Record<string, unknown>>}
        />
      )}
    </div>
  );
}
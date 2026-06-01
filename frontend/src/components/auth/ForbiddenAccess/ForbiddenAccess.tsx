import { Link } from "react-router-dom";
import "./ForbiddenAccess.scss";

type ForbiddenAccessProps = {
  title?: string;
  message?: string;
  requiredRole?: string;
};

export default function ForbiddenAccess({
  title = "Access Forbidden",
  message = "You do not have permission to access this page.",
  requiredRole = "Authorized user",
}: ForbiddenAccessProps) {
  return (
    <section className="forbidden-access">
      <div className="forbidden-access__card">
        <div className="forbidden-access__code">403</div>

        <h1 className="forbidden-access__title">{title}</h1>

        <p className="forbidden-access__message">{message}</p>

        <p className="forbidden-access__role">
          Required role: <strong>{requiredRole}</strong>
        </p>

        <div className="forbidden-access__actions">
          <Link to="/dashboard" className="forbidden-access__button">
            Back to Dashboard
          </Link>

          <Link
            to="/events"
            className="forbidden-access__button forbidden-access__button--secondary"
          >
            Browse Events
          </Link>
        </div>
      </div>
    </section>
  );
}
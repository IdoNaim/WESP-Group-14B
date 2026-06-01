import { NavLink } from 'react-router-dom';
import { useAuth } from '../../../context/AuthContext';
import { navItems, type NavItem } from '../navItems';
import "./Sidebar.scss";

function canShowItem(
    item: NavItem,
    auth: {
        isMember: boolean;
        isProductionUser: boolean;
        isAdmin: boolean;
    }
): boolean {
    switch (item.visibility) {
        case 'all':
            return true;
        case 'member':
            return auth.isMember;
        case 'production':
            return auth.isProductionUser;
        case 'admin':
            return auth.isAdmin;
        default:
            return false;
    }
}


export default function Sidebar() {
    const auth = useAuth();

    const visibleNavItems = navItems.filter(item => canShowItem(item, auth));

      return (
        <aside className="sidebar">
        <nav className="sidebar__nav">
            {visibleNavItems.map((item) => (
            <NavLink
                key={item.path}
                to={item.path}
                className={({ isActive }) =>
                [
                   "sidebar__link",
                    isActive ? "sidebar__link--active" : "",
                ]
                .filter(Boolean)
                .join(" ")
                }
            >
                {item.label}
            </NavLink>
            ))}
        </nav>
        </aside>
    );
}
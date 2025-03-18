import React from 'react';
import { NavLink } from 'react-router-dom';

interface NavItemProps {
  to: string;
  label: string;
}

const NavItem: React.FC<NavItemProps> = ({ to, label }) => {
  return (
    <NavLink
      to={to}
      className={({ isActive }) =>
        `px-4 py-2 rounded-md transition-colors ${
          isActive
            ? 'bg-blue-100 text-blue-700 font-medium'
            : 'text-gray-700 hover:bg-gray-100'
        }`
      }
    >
      {label}
    </NavLink>
  );
};

export default NavItem;
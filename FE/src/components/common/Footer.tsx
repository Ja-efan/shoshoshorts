import React from "react";
import { Link } from "react-router-dom";
import { Logo } from "../common/Logo";

interface FooterLinkGroup {
  title: string;
  links: {
    text: string;
    url: string;
  }[];
}

export const Footer: React.FC = () => {
  const linkGroups: FooterLinkGroup[] = [
    {
      title: "Product",
      links: [
        { text: "Features", url: "#" },
        { text: "Pricing", url: "#" },
        { text: "Examples", url: "#" },
        { text: "Testimonials", url: "#" },
      ],
    },
    {
      title: "Resources",
      links: [
        { text: "Help Center", url: "#" },
        { text: "Blog", url: "#" },
        { text: "Tutorials", url: "#" },
        { text: "Contact", url: "#" },
      ],
    },
    {
      title: "Legal",
      links: [
        { text: "Terms of Service", url: "#" },
        { text: "Privacy Policy", url: "#" },
        { text: "Cookie Policy", url: "#" },
      ],
    },
  ];

  return (
    <footer className="border-t bg-white py-12">
      <div className="container px-4">
        <div className="grid gap-8 md:grid-cols-4">
          <div>
            <Logo size="sm" />
            <p className="mt-2 text-sm text-gray-600">Turn your stories into engaging videos with AI</p>
          </div>

          {linkGroups.map((group, index) => (
            <div key={index}>
              <h3 className="font-semibold">{group.title}</h3>
              <ul className="mt-2 space-y-2 text-sm text-gray-600">
                {group.links.map((link, linkIndex) => (
                  <li key={linkIndex}>
                    <Link to={link.url} className="hover:text-red-600">
                      {link.text}
                    </Link>
                  </li>
                ))}
              </ul>
            </div>
          ))}
        </div>

        <div className="mt-8 border-t pt-8 text-center text-sm text-gray-600">
          <p>© {new Date().getFullYear()} ShoShoShort. All rights reserved.</p>
        </div>
      </div>
    </footer>
  );
};
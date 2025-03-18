import React, { useState } from 'react';
import { Search } from 'lucide-react';

const SearchBar: React.FC = () => {
  const [isFocused, setIsFocused] = useState(false);

  return (
    <div className={`flex items-center px-2 py-1 bg-gray-100 rounded-md transition-all duration-300 ${isFocused ? 'w-64' : 'w-40'}`}>
      <Search className="h-4 w-4 text-gray-500 mr-2" />
      <input
        type="text"
        placeholder="검색..."
        className="bg-transparent border-none outline-none w-full text-sm"
        onFocus={() => setIsFocused(true)}
        onBlur={() => setIsFocused(false)}
      />
    </div>
  );
};

export default SearchBar;

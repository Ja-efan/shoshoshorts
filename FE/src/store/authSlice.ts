// src/redux/authSlice.ts
import { createSlice, PayloadAction } from "@reduxjs/toolkit";

interface AuthState {
  token: string | null;
  isAuthenticated: boolean;
}

// localStorage에서 초기값 가져오기
const initialState: AuthState = {
  token: localStorage.getItem("accessToken"),
  isAuthenticated: !!localStorage.getItem("accessToken"),
};

const authSlice = createSlice({
  name: "auth",
  initialState,
  reducers: {
    setToken: (state, action: PayloadAction<string>) => {
      state.token = action.payload;
      state.isAuthenticated = true;
      localStorage.setItem("accessToken", action.payload);
    },
    clearToken: (state) => {
      state.token = null;
      state.isAuthenticated = false;
      localStorage.removeItem("accessToken");
    },
  },
});

export const { setToken, clearToken } = authSlice.actions;
export default authSlice.reducer;

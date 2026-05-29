import { create } from 'zustand'

type AuthState = {
  accessToken: string | null
  email: string | null
  setTokens: (accessToken: string, email?: string) => void
  clearTokens: () => void
}

export const useAuthStore = create<AuthState>((set) => ({
  accessToken: null,
  email: null,
  setTokens: (accessToken, email) => set({ accessToken, email: email ?? null }),
  clearTokens: () => set({ accessToken: null, email: null }),
}))

import { create } from 'zustand'

type AuthState = {
  accessToken: string | null
  email: string | null
  initialized: boolean
  setTokens: (accessToken: string, email?: string) => void
  clearTokens: () => void
  markInitialized: () => void
}

export const useAuthStore = create<AuthState>((set) => ({
  accessToken: null,
  email: null,
  initialized: false,
  setTokens: (accessToken, email) => set({ accessToken, email: email ?? null, initialized: true }),
  clearTokens: () => set({ accessToken: null, email: null, initialized: true }),
  markInitialized: () => set({ initialized: true }),
}))

import { createContext, useContext, useState, useCallback, type ReactNode } from 'react';
import type { User, BranchInfo } from '../types';
import api, { authApi } from '../services/api';

interface AuthContextType {
  user: User | null;
  accessToken: string | null;
  isAuthenticated: boolean;
  isLoading: boolean;
  branchId: number | null;
  branches: BranchInfo[];
  login: (email: string, password: string) => Promise<void>;
  logout: () => Promise<void>;
  switchBranch: (branchId: number) => Promise<void>;
}

const AuthContext = createContext<AuthContextType | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(null);
  const [accessToken, setAccessToken] = useState<string | null>(
    () => localStorage.getItem('accessToken'),
  );
  const [branchId, setBranchId] = useState<number | null>(
    () => { const v = localStorage.getItem('branchId'); return v ? Number(v) : null; }
  );
  const [branches, setBranches] = useState<BranchInfo[]>([]);
  const [isLoading, setIsLoading] = useState(false);

  const login = useCallback(async (email: string, password: string) => {
    setIsLoading(true);
    try {
      const { data } = await authApi.login({ email, password });
      localStorage.setItem('accessToken', data.accessToken);
      localStorage.setItem('refreshToken', data.refreshToken);
      if (data.branchId != null) localStorage.setItem('branchId', String(data.branchId));
      setAccessToken(data.accessToken);
      setBranchId(data.branchId);
      setBranches(data.branches || []);
      setUser({ id: data.userId, email: data.email, fullName: data.fullName } as User);
    } finally {
      setIsLoading(false);
    }
  }, []);

  const switchBranch = useCallback(async (newBranchId: number) => {
    const { data } = await api.post('/auth/switch-branch', { branchId: newBranchId });
    localStorage.setItem('accessToken', data.accessToken);
    localStorage.setItem('refreshToken', data.refreshToken);
    localStorage.setItem('branchId', String(data.branchId));
    setAccessToken(data.accessToken);
    setBranchId(data.branchId);
    setBranches(data.branches || []);
  }, []);

  const logout = useCallback(async () => {
    try {
      const refreshToken = localStorage.getItem('refreshToken');
      if (refreshToken) await authApi.logout(refreshToken);
    } catch {
      // incluso si el logout falla en backend, limpiamos igual
    } finally {
      localStorage.removeItem('accessToken');
      localStorage.removeItem('refreshToken');
      localStorage.removeItem('branchId');
      setAccessToken(null);
      setBranchId(null);
      setBranches([]);
      setUser(null);
    }
  }, []);

  return (
    <AuthContext.Provider
      value={{
        user,
        accessToken,
        isAuthenticated: !!accessToken,
        isLoading,
        branchId,
        branches,
        login,
        logout,
        switchBranch,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
}

// eslint-disable-next-line react-refresh/only-export-components
export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth debe usarse dentro de AuthProvider');
  return ctx;
}

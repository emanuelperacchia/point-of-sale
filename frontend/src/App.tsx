import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider } from './context/AuthContext';
import ProtectedRoute from './components/common/ProtectedRoute';
import LoginPage from './pages/LoginPage';
import PosPage from './pages/PosPage';

export default function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <Routes>
          <Route path="/login" element={<LoginPage />} />
          <Route
            path="/pos"
            element={
              <ProtectedRoute>
                <PosPage />
              </ProtectedRoute>
            }
          />
          <Route path="*" element={<Navigate to="/pos" replace />} />
        </Routes>
      </AuthProvider>
    </BrowserRouter>
  );
}

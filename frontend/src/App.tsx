import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider } from './context/AuthContext';
import ProtectedRoute from './components/common/ProtectedRoute';
import LoginPage from './pages/LoginPage';
import PosPage from './pages/PosPage';
import CommissionPage from './pages/CommissionPage';
import PayrollListPage from './pages/PayrollListPage';
import PayrollDetailPage from './pages/PayrollDetailPage';
import RecipePage from './pages/RecipePage';
import ProductionOrderPage from './pages/ProductionOrderPage';
import ProductionOrderDetailPage from './pages/ProductionOrderDetailPage';

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
          <Route
            path="/commissions"
            element={
              <ProtectedRoute>
                <CommissionPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/payroll"
            element={
              <ProtectedRoute>
                <PayrollListPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/payroll/:id"
            element={
              <ProtectedRoute>
                <PayrollDetailPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/recipes"
            element={
              <ProtectedRoute>
                <RecipePage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/production-orders"
            element={
              <ProtectedRoute>
                <ProductionOrderPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/production-orders/:id"
            element={
              <ProtectedRoute>
                <ProductionOrderDetailPage />
              </ProtectedRoute>
            }
          />
          <Route path="*" element={<Navigate to="/pos" replace />} />
        </Routes>
      </AuthProvider>
    </BrowserRouter>
  );
}

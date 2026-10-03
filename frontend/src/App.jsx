import { Navigate, Route, Routes } from "react-router-dom";
import { useAuth } from "./auth.jsx";
import { LibraryPage } from "./pages/LibraryPage.jsx";
import { SignInPage } from "./pages/SignInPage.jsx";
import { SignUpPage } from "./pages/SignUpPage.jsx";

export function App() {
  const { session } = useAuth();

  return (
    <Routes>
      <Route path="/signin" element={session ? <Navigate to="/" replace /> : <SignInPage />} />
      <Route path="/signup" element={session ? <Navigate to="/" replace /> : <SignUpPage />} />
      <Route path="/" element={session ? <LibraryPage /> : <Navigate to="/signin" replace />} />
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}

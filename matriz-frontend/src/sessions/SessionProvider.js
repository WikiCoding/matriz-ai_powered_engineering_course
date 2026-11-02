import { createContext, useContext, useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import {
  getProfile,
  login as signIn,
  logout as signOutSession,
} from "../api/auth";
import Login from "../pages/Login";
import Message from "../components/Message";
import { clearSession, readSession, saveSession } from "./storage";

const SessionContext = createContext(null);

export function useSession() {
  const session = useContext(SessionContext);
  if (!session) throw new Error("SessionProvider is required.");
  return session;
}

export function SessionProvider({ children }) {
  const [tokens, setTokens] = useState(readSession);
  const [profile, setProfile] = useState(null);
  const [error, setError] = useState("");
  const [expired, setExpired] = useState(false);
  const navigate = useNavigate();

  useEffect(() => {
    if (!tokens) return;
    let active = true;
    getProfile(tokens.access_token)
      .then((user) => {
        if (active) setProfile(user);
      })
      .catch((failure) => {
        if (!active) return;
        setError(failure.message);
        if (failure.status === 401) {
          clearSession();
          setTokens(null);
          setProfile(null);
          setExpired(true);
        }
      });
    return () => {
      active = false;
    };
  }, [tokens]);

  async function login(email, password) {
    const nextTokens = await signIn(email, password);
    const user = await getProfile(nextTokens.access_token);
    saveSession(nextTokens);
    setTokens(nextTokens);
    setProfile(user);
    setError("");
    setExpired(false);
    navigate("/");
  }

  async function signOut() {
    try {
      await signOutSession(tokens.access_token, tokens.refresh_token);
      clearSession();
      setTokens(null);
      setProfile(null);
      setError("");
      navigate("/");
    } catch (failure) {
      setError(`${failure.message} Sign out could not finish. Please retry.`);
    }
  }

  return (
    <SessionContext.Provider
      value={{
        token: tokens?.access_token,
        profile,
        error,
        expired,
        login,
        signOut,
      }}
    >
      {children}
    </SessionContext.Provider>
  );
}

export function SessionGate({ children }) {
  const { token, profile, error, expired, login } = useSession();
  if (!token) return <Login login={login} expired={expired} />;
  if (!profile)
    return (
      <main className="container">
        <Message error={error} />
        <p role="status">Loading your profile…</p>
      </main>
    );
  return children;
}

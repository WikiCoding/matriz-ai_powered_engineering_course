import { useState } from "react";
import Field from "../components/Field";
import Message from "../components/Message";
import { useMutation } from "../hooks/useMutation";

function Login({ login, expired }) {
  const [email, setEmail] = useState(""),
    [password, setPassword] = useState("");
  const mutation = useMutation();
  return (
    <main className="login-page">
      <form
        className="login-card"
        onSubmit={(e) => {
          e.preventDefault();
          mutation.run(() => login(email, password));
        }}
      >
        <div className="logo-mark">M</div>
        <h1>Matriz</h1>
        <p className="muted">Projects and tasks, in one place.</p>
        <Message
          error={
            mutation.error ||
            (expired && "Your session has expired. Please sign in again.")
          }
        />
        <Field
          label="Email"
          value={email}
          onChange={setEmail}
          type="email"
          required
          autoComplete="username"
          placeholder="name@company.com"
        />
        <Field
          label="Password"
          value={password}
          onChange={setPassword}
          type="password"
          required
          autoComplete="current-password"
        />
        <button className="primary wide" disabled={mutation.pending}>
          {mutation.pending ? "Signing in…" : "Sign in"}
        </button>
      </form>
    </main>
  );
}
export default Login;

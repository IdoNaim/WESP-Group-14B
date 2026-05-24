import React, { useState } from 'react';
import './LoginPage.css'; // We will create this simple style next!

export default function LoginPage() {
    // 1. React 'State' tracks what the user is typing in real time
    const [email, setEmail] = useState('');
    const [password, setPassword] = useState('');
    const [errorMsg, setErrorMsg] = useState('');

    // 2. This function runs when the user clicks "Sign In"
    const handleLoginSubmit = (event: React.FormEvent) => {
        event.preventDefault(); // Prevents the browser from refreshing the page
        setErrorMsg('');

        // Quick validation checkpoint
        if (!email || !password) {
            setErrorMsg('Please fill in all fields.');
            return;
        }

        console.log("Submitting to our upcoming API Sector:", { email, password });
        // TODO: This is where we will hook up your API function to talk to Java!
    };

    return (
        <div className="login-wrapper">
            <div className="login-card">
                <h2>Event Tickets System</h2>
                <p className="subtitle">Please sign in to your account</p>

                {errorMsg && <div className="error-banner">{errorMsg}</div>}

                <form onSubmit={handleLoginSubmit}>
                    <div className="input-group">
                        <label>Email Address</label>
                        <input
                            type="email"
                            placeholder="name@example.com"
                            value={email}
                            onChange={(e) => setEmail(e.target.value)} // Updates 'email' state
                            required
                        />
                    </div>

                    <div className="input-group">
                        <label>Password</label>
                        <input
                            type="password"
                            placeholder="••••••••"
                            value={password}
                            onChange={(e) => setPassword(e.target.value)} // Updates 'password' state
                            required
                        />
                    </div>

                    <button type="submit" className="login-btn">
                        Sign In
                    </button>
                </form>
            </div>
        </div>
    );
}

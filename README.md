## Google OAuth Credentials Setup

1. Go to the [Google Cloud Console](https://www.google.com/url?sa=E&q=https%3A%2F%2Fconsole.cloud.google.com%2F).
2. Create a new project and navigate to **APIs & Services > Library**.
3. Enable the **Gmail API**.
4. Go to **APIs & Services > Credentials** and click **Create Credentials > OAuth Client ID**.
5. Select **Web Application**.
6. **Authorized Redirect URIs:**
    - For local dev: http://localhost:3000
7. Copy the **Client ID** and **Client Secret** into the application configuration.

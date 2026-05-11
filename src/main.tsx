import React, { useEffect } from 'react'
import ReactDOM from 'react-dom/client'
import { BrowserRouter } from 'react-router-dom'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { useAuth0 } from '@auth0/auth0-react'
import Auth0ProviderWithHistory from './auth/Auth0ProviderWithHistory'
import { setTokenFetcher } from './lib/api'
import App from './App.tsx'
import './index.css'

const queryClient = new QueryClient()

const ApiInitializer: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const { getAccessTokenSilently, isAuthenticated } = useAuth0()

  useEffect(() => {
    if (isAuthenticated) {
      setTokenFetcher(getAccessTokenSilently)
    }
  }, [isAuthenticated, getAccessTokenSilently])

  return <>{children}</>
}

ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <BrowserRouter>
      <Auth0ProviderWithHistory>
        <QueryClientProvider client={queryClient}>
          <ApiInitializer>
            <App />
          </ApiInitializer>
        </QueryClientProvider>
      </Auth0ProviderWithHistory>
    </BrowserRouter>
  </React.StrictMode>,
)

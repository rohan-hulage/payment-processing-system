import { defineConfig, loadEnv } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'

// Proxies browser /api/* → API Gateway. ECONNREFUSED = nothing listening on this URL.
// Override in payment-ui/.env.development: DEV_API_GATEWAY_URL=http://127.0.0.1:8080
const defaultGateway = 'http://127.0.0.1:8080'

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')
  const proxyTarget = env.DEV_API_GATEWAY_URL || defaultGateway

  return {
    plugins: [react(), tailwindcss()],
    server: {
      port: 3000,
      proxy: {
        '/api': {
          target: proxyTarget,
          changeOrigin: true,
          configure(proxy) {
            proxy.on('error', (err) => {
              if ('code' in err && err.code === 'ECONNREFUSED') {
                console.warn(
                  `\n[vite] Cannot reach API Gateway at ${proxyTarget} (${String(err.message)}). ` +
                    'Start the backend (port 8080), e.g. from repo root: `mvn clean package -DskipTests && docker compose up -d`.\n'
                )
              }
            })
          },
        },
      },
    },
  }
})

import { fileURLToPath, URL } from 'node:url'
import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'
import vueDevTools from 'vite-plugin-vue-devtools'

// https://vite.dev/config/
export default defineConfig(({ mode }) => {
  // Load env file based on `mode` in the current working directory.
  const env = loadEnv(mode, process.cwd(), '')
  
  return {
    plugins: [
      vue(),
      ...(mode !== 'test' ? [vueDevTools()] : []),
    ],
    resolve: {
      alias: {
        '@': fileURLToPath(new URL('./src', import.meta.url))
      },
    },
    // Base public path when served in production (default: '/')
    base: '/',
    server: {
      port: 3000,
      proxy: {
        // Proxy API requests to the Spring Boot backend during development
        '/api': {
          target: 'http://localhost:8080',
          changeOrigin: true,
          secure: false,
          ws: true
        }
      },
      // Enable CORS for development
      cors: true
    },
    test: {
      environment: 'jsdom',
      globals: true,
    },
    build: {
      // Output directory relative to the project root
      outDir: '../src/main/resources/static',
      emptyOutDir: true,
      // Generate source maps for production builds
      sourcemap: mode !== 'production',
      // Minify for production builds
      minify: mode === 'production' ? 'esbuild' : false,
      // Configure chunk size warning limit
      chunkSizeWarningLimit: 1000,
      rollupOptions: {
        output: {
          // Split chunks to optimize loading
          manualChunks: {
            'vendor': ['vue', 'vue-router', 'pinia']
          }
        }
      }
    },
    // Environment variables to include in the client-side code
    define: {
      'import.meta.env.VITE_APP_VERSION': JSON.stringify(env.npm_package_version),
      'import.meta.env.VITE_BUILD_TIME': JSON.stringify(new Date().toISOString())
    }
  }
})

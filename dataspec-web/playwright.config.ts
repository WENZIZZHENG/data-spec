import { defineConfig, devices } from '@playwright/test'

const port = Number(process.env.DATASPEC_E2E_PORT ?? 4175)
const baseURL = `http://127.0.0.1:${port}`
const configuredChannel = process.env.DATASPEC_E2E_BROWSER_CHANNEL
const browserChannel = configuredChannel === 'bundled'
  ? undefined
  : configuredChannel || (process.platform === 'win32' ? 'msedge' : undefined)
const channelUse = browserChannel ? { channel: browserChannel } : {}

export default defineConfig({
  testDir: './tests/e2e',
  timeout: 60_000,
  expect: {
    timeout: 10_000
  },
  fullyParallel: false,
  forbidOnly: Boolean(process.env.CI),
  retries: process.env.CI ? 1 : 0,
  workers: 1,
  outputDir: './test-results/e2e',
  reporter: [
    ['list'],
    ['html', { open: 'never', outputFolder: 'playwright-report' }]
  ],
  use: {
    baseURL,
    viewport: { width: 1366, height: 900 },
    screenshot: 'only-on-failure',
    trace: 'retain-on-failure',
    video: 'retain-on-failure',
    ...channelUse
  },
  webServer: {
    command: `pnpm dev --host 127.0.0.1 --port ${port}`,
    url: baseURL,
    timeout: 120_000,
    reuseExistingServer: !process.env.CI
  },
  projects: [
    {
      name: 'desktop',
      use: {
        ...devices['Desktop Chrome'],
        ...channelUse
      }
    }
  ]
})

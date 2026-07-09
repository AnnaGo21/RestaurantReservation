import { createBrowserRouter } from 'react-router-dom'
import { AppShell } from '@/components/layout/AppShell'
import { ForbiddenPage, NotFoundPage } from '@/components/shared/ErrorPage'
import { LoginPage } from '@/features/auth/LoginPage'
import { AnalyticsPage } from '@/features/analytics/AnalyticsPage'
import { CalendarPage } from '@/features/calendar/CalendarPage'
import { DashboardPage } from '@/features/dashboard/DashboardPage'
import { GuestsPage } from '@/features/guests/GuestsPage'
import { ReservationsPage } from '@/features/reservations/ReservationsPage'
import { SettingsPage } from '@/features/settings/SettingsPage'
import { TablesPage } from '@/features/tables/TablesPage'
import { MANAGEMENT_ROLES } from '@/lib/roles'
import { HomeRedirect } from './HomeRedirect'
import { RequireAuth } from './RequireAuth'
import { RequireRole } from './RequireRole'

export const router = createBrowserRouter([
  { path: '/login', element: <LoginPage /> },
  {
    element: <RequireAuth />,
    children: [
      {
        element: <AppShell />,
        children: [
          { index: true, element: <HomeRedirect /> },
          {
            element: <RequireRole roles={MANAGEMENT_ROLES} />,
            children: [
              { path: '/dashboard', element: <DashboardPage /> },
              { path: '/tables', element: <TablesPage /> },
              { path: '/analytics', element: <AnalyticsPage /> },
            ],
          },
          { path: '/reservations', element: <ReservationsPage /> },
          { path: '/calendar', element: <CalendarPage /> },
          { path: '/guests', element: <GuestsPage /> },
          { path: '/settings', element: <SettingsPage /> },
        ],
      },
    ],
  },
  { path: '/403', element: <ForbiddenPage /> },
  { path: '*', element: <NotFoundPage /> },
])

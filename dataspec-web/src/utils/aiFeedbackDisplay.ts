export function aiFeedbackSeverityTagType(severity?: string): 'success' | 'warning' | 'danger' | 'info' {
  if (severity === 'danger' || severity === 'ERROR') {
    return 'danger'
  }
  if (severity === 'warning' || severity === 'WARNING') {
    return 'warning'
  }
  if (severity === 'success') {
    return 'success'
  }
  return 'info'
}

export function aiFeedbackPriorityTagType(priority?: string): 'success' | 'warning' | 'danger' | 'info' {
  if (priority === 'HIGH') {
    return 'danger'
  }
  if (priority === 'MEDIUM') {
    return 'warning'
  }
  return 'info'
}

export function buildAiFeedbackRoute(route?: string | null): string {
  return route && route.trim() ? route : '/ai-replay'
}

export function formatAiFeedbackTime(value?: string): string {
  if (!value) {
    return '-'
  }
  return value.replace('T', ' ').replace(/\.\d+$/, '').slice(0, 19)
}

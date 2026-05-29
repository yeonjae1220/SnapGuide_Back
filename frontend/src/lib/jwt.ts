export function parseJwtEmail(token: string): string | null {
  try {
    const payload = token.split('.')[1]
    const padded = payload.replace(/-/g, '+').replace(/_/g, '/')
    const json = atob(padded.padEnd(padded.length + ((4 - (padded.length % 4)) % 4), '='))
    const claims = JSON.parse(json)
    return typeof claims.sub === 'string' ? claims.sub : null
  } catch {
    return null
  }
}

// Thin wrapper around fetch for the Spring Boot API.
// Every error body has the shape { message, fieldErrors } (see GlobalExceptionHandler).

export class ApiError extends Error {
  constructor(status, body) {
    super(body?.message || `Request failed (${status})`);
    this.status = status;
    this.fieldErrors = body?.fieldErrors || {};
  }
}

let onUnauthorized = () => {};

/** Called by AuthProvider so an expired session sends the user back to the login screen. */
export function setUnauthorizedHandler(handler) {
  onUnauthorized = handler;
}

async function request(method, path, body) {
  let response;
  try {
    response = await fetch(`/api${path}`, {
      method,
      credentials: 'same-origin',
      headers: body !== undefined ? { 'Content-Type': 'application/json' } : {},
      body: body !== undefined ? JSON.stringify(body) : undefined,
    });
  } catch {
    throw new ApiError(0, { message: 'Cannot reach the server. Is the backend running?' });
  }

  if (response.status === 204) return null;
  const data = await response.json().catch(() => null);
  if (!response.ok) {
    if (response.status === 401 && path !== '/auth/login' && path !== '/auth/me') {
      onUnauthorized();
    }
    throw new ApiError(response.status, data);
  }
  return data;
}

export const api = {
  get: (path) => request('GET', path),
  post: (path, body) => request('POST', path, body),
  put: (path, body) => request('PUT', path, body),
  del: (path) => request('DELETE', path),
};

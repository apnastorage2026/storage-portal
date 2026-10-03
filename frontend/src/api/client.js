export class ApiError extends Error {
  constructor(status, body) {
    super(messageOf(body) || "Request failed.");
    this.status = status;
    this.body = body;
    this.fields = fieldsOf(body);
  }
}

export async function api(path, { method = "GET", json, form, token } = {}) {
  const headers = {};
  if (token) {
    headers.Authorization = `Token ${token}`;
  }
  let body;
  if (json !== undefined) {
    headers["Content-Type"] = "application/json";
    body = JSON.stringify(json);
  } else if (form) {
    body = form;
  }

  const response = await fetch(path, { method, headers, body });
  const text = await response.text();
  const parsed = text ? JSON.parse(text) : null;
  if (!response.ok) {
    throw new ApiError(response.status, parsed);
  }
  return parsed;
}

export function applyApiError(error, setErrors, setFormError) {
  const fields = error.fields ?? {};
  setErrors(fields);
  setFormError(Object.keys(fields).length > 0 ? "" : (error.message || "Could not reach the server."));
}

function messageOf(body) {
  if (!body || typeof body !== "object") {
    return "";
  }
  if (typeof body.detail === "string") {
    return body.detail;
  }
  if (Array.isArray(body.non_field_errors)) {
    return body.non_field_errors.join(" ");
  }
  return "";
}

function fieldsOf(body) {
  if (!body || typeof body !== "object") {
    return {};
  }
  const fields = {};
  for (const [key, value] of Object.entries(body)) {
    if (key === "detail" || key === "non_field_errors" || !Array.isArray(value)) {
      continue;
    }
    fields[key] = value.join(" ");
  }
  return fields;
}

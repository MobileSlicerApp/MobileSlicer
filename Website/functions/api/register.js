const MAX_FIELD_LENGTH = 600;
const TOKEN_TTL_SECONDS = 60 * 20;

function json(body, status = 200) {
  return new Response(JSON.stringify(body), {
    status,
    headers: {
      "Content-Type": "application/json; charset=utf-8",
      "Cache-Control": "no-store",
    },
  });
}

function clean(value) {
  return String(value || "").trim().slice(0, MAX_FIELD_LENGTH);
}

async function hmac(secret, payload) {
  const key = await crypto.subtle.importKey(
    "raw",
    new TextEncoder().encode(secret),
    { name: "HMAC", hash: "SHA-256" },
    false,
    ["sign"],
  );
  const signature = await crypto.subtle.sign("HMAC", key, new TextEncoder().encode(payload));
  return btoa(String.fromCharCode(...new Uint8Array(signature)))
    .replaceAll("+", "-")
    .replaceAll("/", "_")
    .replaceAll("=", "");
}

async function insertRegistration(env, data, request, id, createdAt) {
  if (!env.BETA_DB) {
    throw new Error("Missing BETA_DB binding");
  }

  await env.BETA_DB.prepare(
    `INSERT INTO beta_registrations
      (id, email, device, printer, testing, created_at)
     VALUES (?, ?, ?, ?, ?, ?)`,
  )
    .bind(
      id,
      data.email || "",
      data.device,
      data.printer,
      data.testing,
      createdAt,
    )
    .run();
}

export async function onRequestPost({ request, env }) {
  try {
    if (!env.DOWNLOAD_TOKEN_SECRET || env.DOWNLOAD_TOKEN_SECRET.length < 32) {
      return json({ error: "Beta download is not configured yet." }, 503);
    }

    const contentType = request.headers.get("Content-Type") || "";
    if (!contentType.includes("application/json")) {
      return json({ error: "Expected JSON request body." }, 415);
    }

    const body = await request.json();
    const data = {
      email: clean(body.email),
      device: clean(body.device),
      printer: clean(body.printer),
      testing: clean(body.testing),
    };

    if (!data.device || !data.printer || !data.testing) {
      return json({ error: "Device, printer, and testing focus are required before downloading the beta." }, 400);
    }

    const id = crypto.randomUUID();
    const issuedAt = Math.floor(Date.now() / 1000);
    const expiresAt = issuedAt + TOKEN_TTL_SECONDS;
    const createdAt = new Date().toISOString();
    await insertRegistration(env, data, request, id, createdAt);

    const payload = `${id}.${expiresAt}`;
    const signature = await hmac(env.DOWNLOAD_TOKEN_SECRET, payload);

    return json({
      downloadUrl: `/api/download?token=${encodeURIComponent(`${payload}.${signature}`)}`,
      expiresAt,
    });
  } catch (error) {
    console.error(error);
    return json({ error: "Registration failed. Please try again." }, 500);
  }
}

export function onRequestGet() {
  return json({ error: "Method not allowed." }, 405);
}

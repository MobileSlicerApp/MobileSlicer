function notFound() {
  return new Response("Not found", {
    status: 404,
    headers: { "Cache-Control": "no-store" },
  });
}

function forbidden(message = "Download link is invalid or expired.") {
  return new Response(message, {
    status: 403,
    headers: { "Cache-Control": "no-store" },
  });
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

function timingSafeEqual(a, b) {
  if (a.length !== b.length) return false;
  let diff = 0;
  for (let index = 0; index < a.length; index += 1) {
    diff |= a.charCodeAt(index) ^ b.charCodeAt(index);
  }
  return diff === 0;
}

async function registrationExists(env, id) {
  if (!env.BETA_DB) return false;
  const row = await env.BETA_DB.prepare("SELECT id FROM beta_registrations WHERE id = ? LIMIT 1")
    .bind(id)
    .first();
  return Boolean(row);
}

async function apkResponse(context) {
  const { env, request } = context;

  if (env.APK_BUCKET) {
    const object = await env.APK_BUCKET.get("MobileSlicer-beta-debug.apk");
    if (!object) return notFound();
    return new Response(object.body, {
      headers: {
        "Content-Type": "application/vnd.android.package-archive",
        "Content-Disposition": 'attachment; filename="MobileSlicer-beta-debug.apk"',
        "Cache-Control": "private, no-store",
      },
    });
  }

  if (env.ASSETS) {
    const assetUrl = new URL(request.url);
    assetUrl.pathname = "/private/MobileSlicer-beta-debug.apk";
    assetUrl.search = "";
    const asset = await env.ASSETS.fetch(new Request(assetUrl.toString(), request));
    if (!asset.ok) return notFound();
    return new Response(asset.body, {
      headers: {
        "Content-Type": "application/vnd.android.package-archive",
        "Content-Disposition": 'attachment; filename="MobileSlicer-beta-debug.apk"',
        "Cache-Control": "private, no-store",
      },
    });
  }

  return notFound();
}

export async function onRequestGet(context) {
  const { request, env } = context;
  const url = new URL(request.url);
  const token = url.searchParams.get("token") || "";
  const parts = token.split(".");
  if (parts.length !== 3 || !env.DOWNLOAD_TOKEN_SECRET) return forbidden();

  const [id, expiresAtText, signature] = parts;
  const expiresAt = Number(expiresAtText);
  if (!id || !Number.isFinite(expiresAt) || Math.floor(Date.now() / 1000) > expiresAt) {
    return forbidden();
  }

  const payload = `${id}.${expiresAtText}`;
  const expected = await hmac(env.DOWNLOAD_TOKEN_SECRET, payload);
  if (!timingSafeEqual(signature, expected)) return forbidden();
  if (!(await registrationExists(env, id))) return forbidden();

  return apkResponse(context);
}

export function onRequestPost() {
  return new Response("Method not allowed", { status: 405, headers: { "Cache-Control": "no-store" } });
}

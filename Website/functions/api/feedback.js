const MAX_FIELD_LENGTH = 2000;
const MAX_PHOTOS = 4;
const MAX_PHOTO_BYTES = 5 * 1024 * 1024;
const ALLOWED_TYPES = new Set(["Question", "Complaint", "Bug/Issue", "Feedback"]);

function json(body, status = 200) {
  return new Response(JSON.stringify(body), {
    status,
    headers: {
      "Content-Type": "application/json; charset=utf-8",
      "Cache-Control": "no-store",
    },
  });
}

function clean(value, maxLength = MAX_FIELD_LENGTH) {
  return String(value || "").trim().slice(0, maxLength);
}

async function insertFeedback(env, data, photos) {
  if (!env.BETA_DB) {
    throw new Error("Missing BETA_DB binding");
  }

  await env.BETA_DB.prepare(
    `INSERT INTO beta_feedback
      (id, type, title, body, photo_keys, created_at)
     VALUES (?, ?, ?, ?, ?, ?)`,
  )
    .bind(
      data.id,
      data.type,
      data.title,
      data.body,
      JSON.stringify(photos),
      data.createdAt,
    )
    .run();
}

function extensionFor(file) {
  const fromName = file.name?.split(".").pop()?.toLowerCase();
  if (fromName && /^[a-z0-9]{2,5}$/.test(fromName)) return fromName;
  if (file.type === "image/png") return "png";
  if (file.type === "image/webp") return "webp";
  return "jpg";
}

async function storePhotos(env, id, files) {
  if (!files.length) return [];
  if (!env.FEEDBACK_BUCKET) {
    throw new Error("Feedback photo storage is not configured yet.");
  }

  const stored = [];
  for (const [index, file] of files.entries()) {
    if (!file.type.startsWith("image/")) {
      throw new Error("Feedback photos must be image files.");
    }
    if (file.size > MAX_PHOTO_BYTES) {
      throw new Error("Each feedback photo must be 5 MB or smaller.");
    }

    const key = `feedback/${id}/${index + 1}.${extensionFor(file)}`;
    await env.FEEDBACK_BUCKET.put(key, file.stream(), {
      httpMetadata: { contentType: file.type || "application/octet-stream" },
    });
    stored.push({ key, name: file.name || "", type: file.type || "", size: file.size });
  }
  return stored;
}

export async function onRequestPost({ request, env }) {
  try {
    const contentType = request.headers.get("Content-Type") || "";
    if (!contentType.includes("multipart/form-data")) {
      return json({ error: "Expected form submission." }, 415);
    }

    const form = await request.formData();
    const data = {
      id: crypto.randomUUID(),
      type: clean(form.get("type"), 40),
      title: clean(form.get("title"), 160),
      body: clean(form.get("body"), MAX_FIELD_LENGTH),
      createdAt: new Date().toISOString(),
    };

    if (!ALLOWED_TYPES.has(data.type) || !data.title || !data.body) {
      return json({ error: "Feedback type, title, and details are required." }, 400);
    }

    const files = form.getAll("photos").filter((item) => item instanceof File && item.size > 0);
    if (files.length > MAX_PHOTOS) {
      return json({ error: "Please attach 4 images or fewer." }, 400);
    }

    const photos = await storePhotos(env, data.id, files);
    await insertFeedback(env, data, photos);
    return json({ ok: true, id: data.id });
  } catch (error) {
    console.error(error);
    return json({ error: error.message || "Feedback submission failed. Please try again." }, 500);
  }
}

export function onRequestGet() {
  return json({ error: "Method not allowed." }, 405);
}

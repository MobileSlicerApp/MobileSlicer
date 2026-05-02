# MobileSlicer Website

Cloudflare Pages site for `mobileslicer.com`.

## Local Preview

From this folder:

```sh
python3 -m http.server 8788
```

Open `http://localhost:8788`.

## Cloudflare Pages

Use `Website` as the project root. No build command is required, and the output directory is `.`.

## Beta Backend

The beta form posts to `/api/register`, stores the submitted setup in D1, then returns a short-lived signed download URL for `/api/download`.

Create the D1 database:

```sh
wrangler d1 create mobileslicer_beta
```

Copy the returned `database_id` into `wrangler.toml`, then apply the schema:

```sh
wrangler d1 execute mobileslicer_beta --file=schema/beta_registrations.sql
```

If the database was created before email became optional, apply the migration:

```sh
wrangler d1 execute mobileslicer_beta --file=schema/002_optional_email.sql
```

Apply the feedback table migration when enabling beta feedback:

```sh
wrangler d1 execute mobileslicer_beta --file=schema/003_beta_feedback.sql
```

Set the signing secret:

```sh
wrangler pages secret put DOWNLOAD_TOKEN_SECRET
```

Use a random value of at least 32 characters.

## APK Storage

Production should use the configured R2 binding:

```sh
wrangler r2 bucket create mobileslicer-beta
wrangler r2 object put mobileslicer-beta/MobileSlicer-beta-debug.apk --file=private/MobileSlicer-beta-debug.apk
```

The download Function prefers R2. If R2 is not bound, it falls back to the private Pages asset path for development.

Feedback screenshots use a separate R2 bucket:

```sh
wrangler r2 bucket create mobileslicer-feedback
```

## Domain Polish

`_redirects` includes:

* `www.mobileslicer.com` to `mobileslicer.com`
* short `/download`, `/privacy`, `/roadmap`, and `/faq` routes
* blocked direct access to `/private/*`

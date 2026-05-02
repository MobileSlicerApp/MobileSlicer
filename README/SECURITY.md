# Security Checklist

This project is currently safer by simplification than a cloud slicer because
the intended product is:

* fully local
* account-free
* backend-service-free

That does **not** mean the app is "secure from hacking" in any strong or
audited sense. This checklist exists so Mobile Slicer can become meaningfully
safer over time instead of assuming local-first design solves everything.

## Security Position

Current honest position:

* local-first reduces many common cloud risks
* native parsing and native dependencies still create a real attack surface
* future printer-network features will increase risk materially
* the app should not be marketed as formally secure until it has real review

## Non-Negotiable Rules

* do not claim the app is security-audited unless that has actually happened
* do not add network/device integrations without a bounded security review
* do not add import formats without treating them as untrusted input
* do not store secrets casually in logs, screenshots, or exported debug files
* do not broaden permissions without a documented reason

## Threat Model Checklist

Review this whenever a meaningful new capability lands.

Assets to protect:

* user-imported model files
* generated G-code files
* local profile/config data
* saved printer-host credentials or API keys
* printer/device control actions

Likely attacker inputs:

* malformed `STL`
* malformed `3MF`
* malformed future `STEP`
* malformed imported profile/config bundles
* malicious shared-file intents / URIs
* hostile or compromised printer-host responses once network features exist

Primary risks:

* native crashes or memory corruption from malformed input
* path traversal / unsafe file handling
* overbroad URI/file exposure
* accidental secret leakage through logs or exported artifacts
* unsafe printer control once upload/start-print features land

## File Import Hardening

Every import path should follow this checklist:

* treat all files as untrusted input
* validate file size and obvious structural bounds before deep parsing
* reject impossible counts / lengths / indexes early
* avoid loading large files into memory all at once when streaming is feasible
* fail closed with clear user-visible errors instead of undefined behavior
* avoid trusting filename extension alone
* prefer content/structure validation where practical
* keep import parsing isolated from UI assumptions

Format-specific rule:

* `STEP` is a later feature and must be treated as security-sensitive because it
  will expand native parsing and dependency surface significantly

## Native / JNI Hardening

Every wrapper/JNI change should be checked for:

* explicit null / bounds checking at boundaries
* no exceptions crossing C or JNI boundaries
* no reliance on unchecked string formats or implicit enum coercion
* clear ownership of returned buffers and native resources
* no silent fallback behavior that hides malformed input
* defensive handling of very large geometry / triangle counts / layer counts

Before claiming a native path is stable, check:

* malformed input does not trivially crash the process
* errors surface as bounded failures, not generic silent false returns
* logging does not expose secrets or unsafe raw payloads unless necessary

## Dependency Checklist

For native and imported third-party dependencies:

* document why the dependency exists
* keep versions explicit where practical
* avoid carrying unnecessary modules just because upstream ships them
* prefer smallest viable imported surface
* track security-sensitive additions in `README/CHANGELOG.md`
* treat new parsers / CAD libraries / networking stacks as security-relevant

High-sensitivity dependency examples:

* Orca / libslic3r native parsing paths
* `3MF` handling
* future `STEP` / OCCT import
* printer-network client libraries

## Android App Checklist

Before shipping a meaningful release:

* review manifest permissions and remove anything not required
* review exported activities/providers/services/receivers
* keep file-provider scope as narrow as possible
* avoid world-readable temp files
* avoid keeping sensitive artifacts longer than necessary
* ensure share/export flows grant only minimal URI access
* avoid writing sensitive data to public storage by default unless the feature explicitly requires it

## Logging And Artifacts

Rules:

* do not log API keys, auth tokens, printer credentials, or full secret-bearing URLs
* do not leave debug dumps enabled by default in release builds
* keep proof artifacts in bounded local directories
* sanitize any future uploaded bug-report bundles

When using proof artifacts:

* separate product-proof artifacts from sensitive runtime data
* do not assume `/tmp` artifacts are a security boundary
* document if a proof run touched credentials or real printer hosts

## Printer / Network Workflow Checklist

This becomes mandatory before direct printer send ships.

For each supported printer host:

* define exact API/auth model
* store credentials minimally
* avoid plaintext logging of tokens/keys
* fail safely on upload/start-print errors
* require explicit user action before starting a print
* separate "upload file" from "start print" unless product intentionally combines them
* verify host/response parsing against malformed or hostile responses

First supported hosts should remain narrow:

* OctoPrint
* Moonraker / Klipper

Do not attempt broad ecosystem support before those are well understood.

Current Mobile Slicer printer-network hardening status:

* Android declares `INTERNET` and `CAMERA` only for local printer API/WebView
  and camera workflows. `CHANGE_WIFI_MULTICAST_STATE` is declared for
  local-network mDNS/Bonjour printer discovery.
* Runtime upload/test support now covers multiple Orca-mapped host types, but
  each host is added only with documented endpoint/auth behavior and bounded
  error handling.
* Printer API keys, usernames, and passwords are migrated out of normal profile
  JSON/preferences into an Android Keystore-backed encrypted preference file.
  Profile and saved-project JSON persistence writes blank secret fields.
* Native slice config generation now omits printer API keys, usernames, and
  passwords because slicing does not need printer credentials.
* Cleartext `http://` printer API calls are refused for public-looking hosts;
  they are allowed only for loopback, RFC1918/private IPs, link-local IPs,
  `.local`, and single-label LAN hostnames. Non-local hosts must use HTTPS.
  The central printer `openConnection(...)` path enforces the same rule
  immediately before network I/O, so helper clients cannot bypass the base URL
  validation accidentally.
* Printer upload/test errors redact URL query/fragment/userinfo before showing
  them to the user.
* The in-app printer WebView blocks top-level navigation away from trusted
  printer/local addresses and only proceeds through TLS warnings for local
  printer addresses.
* The in-app printer WebView disables file/content URL access. JavaScript,
  cookies, mixed HTTP content, and camera permission remain enabled because
  common local printer dashboards and MJPEG streams require them.
* Android app backup and data extraction are disabled in the manifest, with
  explicit data-extraction rules excluding preferences, databases, files, and
  root-domain app data from cloud backup and device transfer.
* Manifest cleartext remains enabled only because Android manifest/network
  security XML cannot express arbitrary user-entered private LAN printer hosts.
  Runtime printer networking refuses cleartext `http://` for non-local hosts in
  app code and has unit coverage for local-vs-public cleartext decisions.
* Direct camera access uses a dedicated `Camera` action because embedded
  dashboard camera panels can fail inside Android WebView.
* Cloud printer hosts are implemented only when their auth and upload surface
  is explicit in Orca source. SimplyPrint follows Orca's OAuth/PKCE parameters,
  stores access/refresh tokens through the encrypted printer credential path,
  and mirrors Orca's chunked upload cleanup path when a chunk fails.

Remaining required hardening:

* profile export now explicitly omits printer API keys, Bambu access codes,
  usernames, and passwords by default and marks the export payload with
  `includesPrinterSecrets=false`. A future "include secrets" export mode would
  need a separate explicit user action.
* Bambu LAN is currently limited to saved setup and reachability checks. Direct
  upload/start remains disabled until the secure MQTT plus FTP/FTPS sequence can
  be validated on hardware.
* upload progress and cancel UI are implemented for active sends, and failed
  uploads now keep the last send request available for a one-tap retry.
  More granular timeout/auth/TLS messaging is still needed.
* release-mode logging still needs a final pre-RC pass across native slicer and
  UI logs, but the current printer-network audit found no direct logging of
  printer API keys, Bambu access codes, usernames, passwords, bearer tokens, or
  full secret-bearing URLs in the printer connection path.

## Release Checklist

Before any public release candidate:

* run a bounded import-fuzz sanity pass on supported file types
* run `scripts/verify_android.sh stubs`, `scripts/verify_android.sh lint`, and
  `scripts/verify_android.sh unit`
* run `scripts/verify_android.sh local` once the native APK build is green
* run `scripts/verify_android.sh release` before release-candidate packaging
* run the release-hardening unit tests that lock down backup, data extraction,
  exported components, and FileProvider scope
* review `AndroidManifest.xml` permissions and exported components against the
  current product surface
* build, sign, install, and smoke-test the release variant on physical hardware
* verify model import, workspace rendering, slicing, G-code export/share, and
  printer upload flows on the signed release build
* review crash logs for malformed-file failure paths
* review manifest and exported component surface
* review native dependency changes since last release
* review logging for credential or path leakage
* review share/export intent behavior
* confirm release builds do not expose developer/debug-only UI or endpoints

## Later Security Milestones

These are meaningful later goals, not current claims:

* lightweight threat model review per major feature tranche
* targeted malformed-file import testing
* release-mode logging audit
* network-printer auth/storage audit
* future third-party security review once the app is materially broader

## Public Wording Rule

Safe wording:

* the app is local-first, which reduces many common cloud risks
* security hardening is ongoing
* the app has not yet had a formal security audit

Unsafe wording:

* secure from hacking
* hardened
* audited
* safe for sensitive environments

Do not use those stronger claims unless they become true.

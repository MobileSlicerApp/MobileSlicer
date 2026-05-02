# Printer Connection

This document tracks the printer connection feature for Mobile Slicer. The
Android app stores Orca-style printer connection profile fields and now has
runtime paths for manually entered Octo/Klipper, PrusaLink, PrusaConnect, Duet,
Repetier, ESP3D, CrealityPrint, FlashAir, AstroBox, MKS, Flashforge, Obico,
SimplyPrint, Elegoo Link, and guarded Bambu LAN setup.

## Current App Status

Implemented:

* `Printer > Connection` is a dedicated profile editor tab.
* The tab stores/imports/exports the Orca connection fields that are already
  surfaced in the app.
* These controls remain visible without enabling `Advanced slicer controls`,
  because connecting to a printer is an app workflow, not slicer tuning.
* Android declares `INTERNET` permission for local printer communication.
* `Test Connection` is available from `Printer > Connection`.
  * For `PrusaLink`, Mobile Slicer follows Orca's `OctoPrint.cpp` PrusaLink
    path: `GET /api/version`, validate `PrusaLink` / `OctoPrint` response
    text, and read `capabilities.upload-by-put`.
  * For `PrusaConnect`, Mobile Slicer follows Orca's `OctoPrint.cpp`
    PrusaConnect subclass: same version/probe path as PrusaLink with
    PrusaConnect-specific post-upload form fields.
  * For `Octo/Klipper`, Mobile Slicer probes OctoPrint with `GET /api/version`.
  * If OctoPrint does not respond, it probes Moonraker with `GET /server/info`.
  * For `Duet`, Mobile Slicer follows Orca's `Duet.cpp` behavior: try classic
    RepRapFirmware `rr_connect` first, then fall back to DSF `machine/status`.
  * For `Repetier`, Mobile Slicer follows Orca's `Repetier.cpp` behavior:
    `GET /printer/info` with `X-Api-Key` authentication.
  * For `ESP3D`, Mobile Slicer follows Orca's `ESP3D.cpp` behavior:
    `GET /command?plain=M105`.
  * For `CrealityPrint`, Mobile Slicer follows Orca's `CrealityPrint.cpp`
    behavior: `GET /info` with bearer-token authentication.
  * For `Elegoo Link`, Mobile Slicer follows Orca's `ElegooLink.cpp`
    behavior: try OctoPrint compatibility first, then detect native Elegoo Link
    from a root response containing `ELEGOO`.
  * For `FlashAir`, Mobile Slicer follows Orca's `FlashAir.cpp` behavior:
    `GET /command.cgi?op=118` verifies that upload is enabled.
  * For `AstroBox`, Mobile Slicer follows Orca's `AstroBox.cpp` behavior:
    `GET /api/version` with optional `X-Api-Key` authentication.
  * For `MKS`, Mobile Slicer follows Orca's `MKS.cpp` behavior:
    send `M105` to the TCP console.
  * For `Flashforge`, Mobile Slicer follows Orca's `Flashforge.cpp` behavior:
    send `~M601 S1` to the TCP console.
  * For `Obico`, Mobile Slicer follows Orca's `Obico.cpp` behavior:
    `GET /api/v1/version/` with `Authorization: Bearer {token}`.
  * For `SimplyPrint`, Mobile Slicer follows the token-authenticated portions
    of Orca's `SimplyPrint.cpp`: `GET /oauth2/TokenInfo` with
    `Authorization: Bearer {token}`.
  * For `Bambu LAN`, Mobile Slicer follows Orca's non-`PrintHost` boundary:
    it stores the local IP, access code, and device serial in profile fields,
    then performs guarded reachability checks for Orca's secure LAN MQTT/FTPS
    ports. Direct upload/start remains disabled until the MQTT plus FTPS flow
    is validated on hardware.
* `Discover Printers` uses Android NSD/mDNS to look for printer-specific local
  services, including OctoPrint, Moonraker, PrusaLink, and Bambu LAN
  service-name candidates. It fills the host field from a picker; manual entry
  remains supported. Generic `_http._tcp` discovery is intentionally excluded
  because it surfaces unrelated devices such as inkjet printers.
  If mDNS does not find the printer, the same user-triggered action also runs a
  bounded local subnet probe for known printer API endpoints, currently
  Moonraker `/server/info`, OctoPrint `/api/version`, and PrusaLink
  `/api/version` on common local ports. The scan only adds a candidate after a
  printer-specific API response, so generic web devices are not listed.
* `Refresh Status` reports printer state, file, progress percent when the host
  provides it, and available nozzle/bed temperatures. The status dialog
  auto-refreshes while it is open.
* Printer controls appear in the Preview top bar after a successful slice.
  * If no printer host or Device UI is configured, `Send` and `Printer` stay
    visible but show a setup prompt that points the user to
    `Profiles > Printer > Connection`.
  * When a printer host is configured, Preview polls status and shows a compact
    top-bar indicator such as `Printing 42%`.
  * PrusaLink uploads use Orca's capability split: newer hosts use raw
    `PUT /api/v1/files/{storage}/{filename}` with `Overwrite: ?1` and optional
    `Print-After-Upload: ?1`; older hosts fall back to multipart
    `POST /api/files/{storage}` with `path`, `print`, and `file`.
  * PrusaConnect uploads reuse the PrusaLink family upload flow. For multipart
    POST, upload-and-start sends Orca's `to_print=True`, upload-to-queue sends
    Orca's `to_queue=True`, and upload-only sends no post-action form field.
  * OctoPrint uploads use multipart `POST /api/files/local`.
  * Moonraker uploads use multipart `POST /server/files/upload`.
  * Duet classic RepRapFirmware uploads use `POST /rr_upload`; DSF uploads use
    `PUT /machine/file/gcodes/{filename}`.
  * Repetier uploads use multipart `POST /printer/model/{slug}` for upload-only
    and `POST /printer/job/{slug}` with `autostart=true` for upload-and-start.
  * ESP3D uploads use multipart `POST /upload_serial` with an Orca-style 8.3
    upload filename; upload-and-start sends `M23` then `M24`.
  * CrealityPrint uploads use Orca's multipart
    `POST /upload/{safe_filename}` path with `Authorization: Bearer {token}`,
    field `path`, and file field `file`; upload-and-start sends Orca's
    `opGcodeFile` WebSocket command on port `9999`.
  * Elegoo Link uploads use Orca's native `POST /uploadFile/upload` chunk flow
    with 1 MiB file chunks, `Check`, `S-File-MD5`, `Offset`, `Uuid`,
    `TotalSize`, and file field `File`; upload-and-start opens
    `ws://{host}:3030/websocket`, waits for file-check status to clear, then
    sends SDCP start command `128`.
  * FlashAir uploads use the official `upload.cgi` sequence:
    write-protect/time setup, upload directory selection, then multipart upload.
    FlashAir is upload-only; remote start is not supported by Orca's host action.
  * AstroBox uploads use multipart `POST /api/files/local` with `print=true`
    for upload-and-start.
  * MKS uploads use raw HTTP `POST /upload?X-Filename={filename}`; upload-and-
    start sends `M23 {filename}` then `M24` over the TCP console.
  * Flashforge uploads use Orca's TCP serial flow on port `8899`: setup
    commands, `~M28 {size} 0:/user/{filename}`, raw file chunks, `~M29`, and
    optionally `~M23 0:/user/{filename}`.
  * Obico uploads use multipart `POST /api/v1/g_code_files/` with Orca's
    `print`, `path`, `printer_id`, `filename`, and `file` fields. Blank host
    defaults to `https://app.obico.io`; `Printer path or port` stores the
    selected Obico printer id.
  * SimplyPrint uploads use Orca's temp upload paths: small files use multipart
    `POST https://simplyprint.io/api/files/TempUpload`; files over 100 MB use
    Orca's `ChunkReceive` sequence and then complete TempUpload with `chunkId`.
    The app opens the SimplyPrint import URL built from the returned temp UUID.
    Mobile Slicer exposes this as upload-only / upload-to-queue, not direct
    print start.
  * `Send` opens upload choices for upload-only or upload-and-start and lets
    the user edit the remote `.gcode` filename before uploading.
  * Active uploads show percent progress and can be cancelled from the upload
    dialog.
  * `Printer` opens `Device UI` when set, otherwise the configured host, in an
    in-app WebView browser.
  * `Camera` in the in-app printer browser uses Moonraker's standard
    `GET /server/webcams/list` endpoint when available, resolves the first
    enabled webcam's `stream_url`, and falls back to `/webcam/?action=stream`
    when no webcam entry is available. This is intended for Klipper printers
    generally, not just Qidi.
  * The camera view keeps the raw MJPEG stream URL in WebView and exposes a
    `Refresh` control. The full dashboard's embedded camera panel may still
    remain blank in Android WebView, so Mobile Slicer treats the direct camera
    stream as the supported path.
  * The WebView allows mixed HTTP content and cookies so printer camera streams
    embedded by Fluidd/Mainsail-style pages can load inside the app.
  * The WebView blocks top-level navigation away from trusted printer/local
    addresses, disables file/content URL access, and only accepts TLS warning
    bypasses for local printer addresses.
  * Printer API calls redact URL userinfo/query/fragment in user-facing errors.
  * Cleartext HTTP printer API calls are restricted to local/private printer
    addresses; non-local hosts must use HTTPS.
  * For Moonraker/Qidi-style hosts, upload tries the configured host and common
    Moonraker ports `7125` and `10088` when no port is specified.
  * Upload normalizes configured hosts to the API origin before probing or
    uploading, so web UI routes such as `/#` are not included in API URLs.

Not implemented:

* host-specific field visibility.
* retry UI and richer post-upload action handling.
* file browser / console integration.
* Bambu LAN upload/start. The current Bambu path is setup and reachability only.

## Orca Source References

Use the vendored Orca source as the behavioral reference:

* `vendor/orcaslicer/src/libslic3r/PrintConfig.cpp`
  * defines saved connection settings:
    `host_type`, `printer_agent`, `print_host`, `print_host_webui`,
    `printhost_authorization_type`, `printhost_apikey`, `printhost_user`,
    `printhost_password`, `printhost_port`, `printhost_cafile`,
    `printhost_ssl_ignore_revoke`, and `bbl_use_printhost`
  * defines Host Type values and labels:
    `PrusaLink`, `PrusaConnect`, `Octo/Klipper`, `Duet`, `FlashAir`,
    `AstroBox`, `Repetier`, `MKS`, `ESP3D`, `CrealityPrint`, `Obico`,
    `Flashforge`, `SimplyPrint`, and `Elegoo Link`
* `vendor/orcaslicer/src/slic3r/GUI/PhysicalPrinterDialog.cpp`
  * builds Orca's Physical Printer dialog
  * wires `Browse`, `Test`, `Login/Test`, `Refresh`, field visibility, default
    cloud host URLs, and auth-specific field display
* `vendor/orcaslicer/src/slic3r/Utils/PrintHost.hpp`
  * defines the `PrintHost` interface:
    `test`, `upload`, `has_auto_discovery`, `can_test`,
    `get_post_upload_actions`, `supports_multiple_printers`, `get_groups`,
    `get_printers`, and `get_storage`
* `vendor/orcaslicer/src/slic3r/Utils/PrintHost.cpp`
  * chooses the concrete print-host implementation from `host_type`
  * owns Orca's upload job queue and common error formatting
* `vendor/orcaslicer/src/slic3r/Utils/OctoPrint.cpp`
  * implements Orca's OctoPrint/PrusaLink-style test and upload behavior
  * PrusaLink-specific behavior includes `api/version`, auth selection,
    `capabilities.upload-by-put`, `api/v1/storage`, `api/v1/files`,
    `api/files`, `Overwrite`, `Print-After-Upload`, and multipart `print`
  * PrusaConnect-specific behavior inherits the PrusaLink upload path and uses
    `Accept-Language`, multipart `to_print`, and multipart `to_queue`
* `vendor/orcaslicer/src/slic3r/Utils/Duet.cpp`
  * implements Orca's Duet runtime behavior:
    `rr_connect` / `rr_upload` / `rr_gcode` for classic RepRapFirmware and
    `machine/status` / `machine/file/gcodes` / `machine/code` for DSF
* `vendor/orcaslicer/src/slic3r/Utils/Repetier.cpp`
  * implements Orca's Repetier runtime behavior:
    `printer/info`, `printer/model/{slug}`, `printer/job/{slug}`,
    `X-Api-Key`, and `printhost_port` as the selected printer slug
* `vendor/orcaslicer/src/slic3r/Utils/ESP3D.cpp`
  * implements Orca's ESP3D runtime behavior:
    `command?plain=M105`, multipart `upload_serial`, 8.3 filenames, then
    `M23` / `M24` for upload-and-start
* `vendor/orcaslicer/src/slic3r/Utils/CrealityPrint.cpp`
  * implements Orca's CrealityPrint runtime behavior:
    `GET /info`, `Authorization: Bearer {token}`, multipart
    `POST /upload/{safe_filename}` with `path` and `file` fields, and
    upload-and-start through a WebSocket `opGcodeFile` command on port `9999`
* `vendor/orcaslicer/src/slic3r/Utils/ElegooLink.cpp`
  * implements Orca's Elegoo Link runtime behavior:
    OctoPrint-compatible fallback test/upload, native Elegoo detection by
    `ELEGOO` root response, chunked `uploadFile/upload`, MD5 and UUID metadata,
    and WebSocket SDCP commands on port `3030`
* `vendor/orcaslicer/src/slic3r/Utils/FlashAir.cpp`
  * implements Orca's FlashAir runtime behavior:
    `command.cgi?op=118`, `upload.cgi?WRITEPROTECT=ON&FTIME=...`,
    `upload.cgi?UPDIR=...`, and multipart upload to `upload.cgi`
* `vendor/orcaslicer/src/slic3r/Utils/AstroBox.cpp`
  * implements Orca's AstroBox runtime behavior:
    `api/version`, `api/files/local`, `X-Api-Key`, `print`, `path`, and `file`
    multipart fields
* `vendor/orcaslicer/src/slic3r/Utils/MKS.cpp`
  * implements Orca's MKS runtime behavior:
    TCP console port `8080`, `M105` test, raw HTTP `upload?X-Filename=...`,
    and `M23` / `M24` upload-and-start
* `vendor/orcaslicer/src/slic3r/Utils/Flashforge.cpp`
  * implements Orca's Flashforge runtime behavior:
    TCP console port `8899`, `~M601 S1`, `~M115`, `~M640` or `~M650`,
    `~M119`, `~M28`, raw 4 KiB file chunks, `~M29`, and `~M23`
* FlashAir official API docs:
  * `https://flashair-developers.github.io/website/docs/api/command.cgi.html`
  * `https://flashair-developers.github.io/website/docs/api/upload.cgi.html`
* AstroPrint forum guidance for local AstroBox upload:
  * `https://forum.astroprint.com/t/upload-file-using-command-line/2929`
* `vendor/orcaslicer/src/slic3r/Utils/QidiPrinterAgent.cpp`
  * shows Qidi-specific behavior is tied to Orca's Moonraker printer-agent
    path, so Qidi must be verified before assuming plain OctoPrint endpoints
* `vendor/orcaslicer/src/slic3r/Utils/BBLPrinterAgent.cpp`,
  `vendor/orcaslicer/src/slic3r/Utils/BBLNetworkPlugin.*`,
  `vendor/orcaslicer/src/slic3r/GUI/Jobs/PrintJob.cpp`, and
  `vendor/orcaslicer/src/slic3r/GUI/SelectMachine.cpp`
  * show that Bambu LAN is not a normal `PrintHost`. Orca collects `dev_ip`,
    `dev_id`, FTP folder, and access code, then uses secure local MQTT and
    FTP/FTPS behavior through the BBL network/plugin path.

## Orca Connection Model

Orca separates connection settings from the slicer settings that affect G-code.
The Physical Printer dialog stores connection values in the printer profile,
then uses those values only when a user tests the host or sends sliced output.

The high-level flow is:

1. User edits physical printer connection settings.
2. Orca chooses a `PrintHost` implementation from `host_type`.
3. `Test` calls the selected host implementation's `test()` method.
4. After slicing/export, Orca creates a `PrintHostUpload` with the local G-code
   path, upload filename/path, optional group/storage, and post-upload action.
5. Orca enqueues the upload and reports progress/errors through the print-host
   queue.

Mobile Slicer should mirror this model conceptually, but should not port Orca's
wxWidgets/curl GUI stack directly into Android. The Android implementation
should use Kotlin networking clients and keep Orca as the source reference for
field names, labels, host behavior, and profile compatibility.

## Host Types

All Orca host types should remain import/export compatible. Runtime support can
ship incrementally and must be documented per host.

| Orca host label | Config value | Android runtime target |
| --- | --- | --- |
| PrusaLink | `prusalink` | Test, basic status, upload-only, upload-and-start, and Orca-style writable storage browsing implemented from Orca `OctoPrint.cpp`. API-key auth and Orca-style username/password HTTP Digest auth are implemented. Hardware validation still needed. |
| PrusaConnect | `prusaconnect` | Test, basic status, upload-only, upload-and-start, and upload-to-queue implemented from Orca `OctoPrint.cpp`. Defaults blank host to `https://connect.prusa3d.com`. Hardware validation still needed. |
| Octo/Klipper | `octoprint` | Test, status, upload-only, and upload-and-start implemented for OctoPrint and Moonraker. Qidi Q2 upload-only was user-verified; broader device testing still needed. |
| Duet | `duet` | Test, status, upload-only, and upload-and-start implemented from Orca `Duet.cpp` for classic RepRapFirmware and DSF. Hardware validation still needed. |
| FlashAir | `flashair` | Test, basic upload-enabled status, and upload-only implemented from Orca `FlashAir.cpp` plus official FlashAir API docs. Remote start is not supported. Hardware validation still needed. |
| AstroBox | `astrobox` | Test, basic version status, upload-only, and upload-and-start implemented from Orca `AstroBox.cpp`. Hardware validation still needed. |
| Repetier | `repetier` | Test, basic status, upload-only, upload-and-start, Orca-style printer slug browsing, and Orca-style model group browsing implemented from Orca `Repetier.cpp`. Hardware validation still needed. |
| MKS | `mks` | Test, basic status, upload-only, and upload-and-start implemented from Orca `MKS.cpp`. Uses `Printer path or port` as optional TCP console port, defaulting to `8080`. Hardware validation still needed. |
| ESP3D | `esp3d` | Test, basic status, upload-only, and upload-and-start implemented from Orca `ESP3D.cpp`. Hardware validation still needed. |
| CrealityPrint | `crealityprint` | Test, basic status, upload-only, and upload-and-start implemented from Orca `CrealityPrint.cpp`. Uses bearer-token auth and WebSocket start on port `9999`. Hardware validation still needed. |
| Obico | `obico` | Test, basic status, upload-only, upload-and-start, and Orca-style printer id browsing implemented from Orca `Obico.cpp`. Defaults blank host to `https://app.obico.io`. Hardware validation still needed. |
| Flashforge | `flashforge` | Test, basic status, upload-only, and upload-and-start implemented from Orca `Flashforge.cpp`. Uses `Printer path or port` as optional TCP console port, defaulting to `8899`. Hardware validation still needed. |
| SimplyPrint | `simplyprint` | Partial runtime support from Orca `SimplyPrint.cpp`: OAuth/PKCE login, encrypted access/refresh token storage, token refresh for API calls, small temp upload, chunked temp upload, and automatic in-app opening of the SimplyPrint import URL. |
| Elegoo Link | `elegoolink` | Test, basic status, upload-only, and upload-and-start implemented from Orca `ElegooLink.cpp`. OctoPrint-compatible Elegoo hosts use the OctoPrint upload path; native Elegoo chunk upload uses MD5/UUID metadata and WebSocket start on port `3030`. Hardware validation still needed. |
| Bambu LAN | `bambulan` | Mobile Slicer app-specific guarded setup mode based on Orca's BBL network boundary, not a normal Orca `PrintHost`. Stores IP, access code, and device serial, and checks secure LAN MQTT/FTPS reachability. Upload/start intentionally disabled pending hardware validation. |

## Android Implementation Plan

Host support roadmap:

1. Keep Octo/Klipper as the known working baseline.
2. Add Duet/RepRapFirmware from Orca `Duet.cpp`. Implemented; needs hardware
   validation.
3. Add Repetier from Orca `Repetier.cpp`. Implemented for upload/status plus
   Orca-style `printer/list` browsing into `printhost_port` and
   `listModelGroups` browsing into `printhost_group`.
4. Add simple embedded/SD-card hosts where Orca exposes direct HTTP or serial
   protocols. ESP3D, CrealityPrint, FlashAir, AstroBox, MKS, and Flashforge are
   implemented.
5. Add cloud or account-backed hosts only after the local-host path is stable:
   PrusaConnect and Obico are implemented; SimplyPrint has partial OAuth-backed
   runtime support; Elegoo Link is implemented from Orca and needs hardware
   validation.
6. Add Bambu LAN as a separate printer-agent path, not a normal `PrintHost`
   client. Orca routes Bambu through `BBLPrinterAgent` and `BBLNetworkPlugin`,
   with LAN discovery, serial/device ID, access code/password, MQTT, and
   FTP/FTPS upload. Mobile Slicer now has guarded setup/reachability only;
   upload/start remains a future hardware-validated step.

First runtime milestone:

* manual host/IP entry only: implemented.
* `Octo/Klipper` selected in `host_type`: implemented.
* `Test Connection` button: implemented.
* upload generated `.gcode` after a successful slice: implemented.
* upload-only action: implemented under the Preview `Send` sheet and verified
  on the user's Qidi Q2 path.
* upload-and-start action: implemented under the Preview `Send` sheet.
* pre-upload remote filename editing: implemented.
* upload progress and cancellation: implemented for the Android upload stream.
* printer UI open action: implemented as `Printer` in the Preview top controls
  with an in-app WebView browser.
* connection editor `Open UI` action: implemented.
* connection editor status refresh: implemented for Moonraker/Klipper and basic
  OctoPrint job state.
* Duet test/upload/status: implemented from Orca's classic RepRapFirmware and
  DSF paths, without hardware validation yet.
* Repetier test/upload/status: implemented from Orca's Repetier Server HTTP
  paths, using `printhost_apikey` as `X-Api-Key` and `printhost_port` as the
  printer slug. Hardware validation still needed.
* ESP3D test/upload/status: implemented from Orca's HTTP path, including
  8.3 filename shortening and upload-and-start through `M23` / `M24`. Hardware
  validation still needed.
* CrealityPrint test/upload/status: implemented from Orca's `CrealityPrint.cpp`
  path, including `GET /info`, bearer-token auth, multipart upload to
  `/upload/{safe_filename}`, and upload-and-start through the port `9999`
  WebSocket `opGcodeFile` command. Hardware validation still needed.
* Elegoo Link test/upload/status: implemented from Orca's `ElegooLink.cpp`
  native path, including root `ELEGOO` detection, 1 MiB multipart chunks to
  `/uploadFile/upload`, file MD5, upload UUID, `code == 000000` response
  validation, and upload-and-start through the port `3030` WebSocket SDCP
  command. Hardware validation still needed.
* FlashAir test/upload/status: implemented from Orca's FlashAir host path and
  official FlashAir `command.cgi` / `upload.cgi` docs. Hardware validation still
  needed, and remote start is intentionally unavailable.
* AstroBox test/upload/status: implemented from Orca's AstroBox host path.
  Hardware validation still needed.
* MKS test/upload/status: implemented from Orca's MKS host path, including TCP
  console `M105`, raw HTTP upload, and `M23` / `M24` upload-and-start. Hardware
  validation still needed.
* Flashforge test/upload/status: implemented from Orca's Flashforge host path,
  including TCP console setup, raw chunk upload, save, and optional start.
  Hardware validation still needed.
* Obico test/upload/status: implemented from Orca's Obico host path, including
  Bearer-token authentication, blank-host defaulting to `https://app.obico.io`,
  printer listing for status, and multipart upload with the configured printer
  id. Hardware validation still needed.
* SimplyPrint test/upload/status: partially implemented from Orca's SimplyPrint
  host path, including OAuth/PKCE login, encrypted access/refresh token storage,
  TokenInfo status, token refresh for API calls, small temp upload, and chunked
  temp upload for files over 100 MB. Successful uploads open the in-app browser
  to Orca's `panel?import=tmp:{uuid}&filename={filename}` import URL.
* direct camera stream action: implemented as `Camera` inside the in-app printer
  browser; it prefers Moonraker webcam configuration and falls back to the
  common `/webcam/?action=stream` route. Verified working for the user's Qidi
  Q2 stream route.
* Qidi still needs verification against Orca's `QidiPrinterAgent` / Moonraker
  path before claiming device support.

Suggested Kotlin boundary:

* `PrinterConnectionClient`
  * `testConnection(profile): PrinterConnectionResult`
  * `uploadGcode(profile, file, postAction, progress): UploadResult`
  * `canBrowse`, `canTest`, `supportedPostActions`,
    `supportsMultiplePrinters`
* host clients:
  * `OctoPrintConnectionClient`
  * `MoonrakerConnectionClient`
  * `PrusaLinkConnectionClient`
  * additional clients as host support is added
* `PrinterConnectionRepository`
  * resolves `host_type` / `printer_agent`
  * normalizes user-entered URLs/IPs
  * owns user-facing error mapping
* background upload:
  * use coroutines or WorkManager
  * expose progress, cancellation, retry, and final error text

Do not route printer network operations through the native slicing wrapper
unless a specific host requires native code. Uploading a generated file is an
Android app concern; slicing/export remains the native engine concern.

## Field Visibility Rules

The Android `Connection` tab should mirror Orca's Physical Printer dialog:

* `Host Type` is shown for FFF printers.
* `Printer Agent` is shown with the other connection fields.
* `Hostname, IP or URL` is enabled for normal local hosts.
* `Device UI` is shown for normal hosts, but Orca hides it for SimplyPrint.
* `Authorization Type` is shown for PrusaLink and SLA-style cases.
* `API Key / Password` is shown for API-key auth and most non-PrusaLink hosts.
* `User` and `Password` are shown for user/password auth.
* `Printer` / `printhost_port` is shown only for hosts that support multiple
  printers or storage targets. Mobile Slicer now includes Orca-style Browse
  actions for Repetier `printer/list`, Repetier `listModelGroups`, Obico
  `api/v1/printers/`, and PrusaLink `api/v1/storage`.
* `HTTPS CA File` is relevant for HTTPS/self-signed certificate support.
* `Ignore HTTPS certificate revocation checks` is an Orca setting, but the
  vendored source comments that it is Windows-only; Android behavior needs its
  own TLS decision instead of blindly copying desktop behavior.
* Flashforge hides API-key/auth fields in Orca.
* PrusaConnect, Obico, and SimplyPrint may seed or lock default cloud URLs.

## Protocol Notes

Implementation must verify each protocol from Orca source and real-device/API
behavior before claiming support.

Known Orca behavior:

* OctoPrint test uses `GET /api/version`.
* OctoPrint upload posts a multipart file to `/api/files/local` with fields
  such as `print`, `path`, and `file`.
* PrusaLink:
  * shares Orca's `OctoPrint.cpp` source file but has different auth, storage,
    and upload-path behavior
  * test: `GET /api/version`; the JSON must include `api`, and `text` must
    start with `PrusaLink` or `OctoPrint`
  * capability selection: `capabilities.upload-by-put=true` uses the newer
    raw PUT path, otherwise the older multipart POST path is used
  * storage: Orca can query `GET /api/v1/storage`; Mobile Slicer defaults to
    `/local` and currently lets `Printer path or port` override the storage
    path manually
  * newer upload: `PUT /api/v1/files/{storage}/{filename}` with
    `Content-Type: text/x.gcode`, `Overwrite: ?1`, and `Print-After-Upload: ?1`
    only for upload-and-start
  * older upload: multipart `POST /api/files/{storage}` with fields `path`,
    `print`, and file field `file`
  * status: `GET /api/v1/status` when available, with fallback to version-only
    online reporting
  * auth: API-key mode sends `X-Api-Key`; username/password mode follows Orca's
    `atUserPassword` path with HTTP Digest auth. Mobile Slicer fetches the
    `WWW-Authenticate: Digest ...` challenge, then builds the MD5 Digest
    `Authorization` header for probe, status, PUT upload, and multipart upload
    requests.
* PrusaConnect:
  * implemented from Orca's `PrusaConnect` subclass in `OctoPrint.cpp`
  * default host: `https://connect.prusa3d.com` when the profile host field is
    blank
  * test/probe: same `GET /api/version` and `capabilities.upload-by-put` path
    as PrusaLink
  * multipart upload-and-start: use `to_print=True` instead of PrusaLink's
    `print=true`
  * multipart upload-only: no post-action field
  * multipart upload-to-queue: use `to_queue=True`; Mobile Slicer exposes this
    queue action only when the active host type is PrusaConnect.
* Obico:
  * implemented from Orca's `Obico.cpp`
  * default host: `https://app.obico.io` when the profile host field is blank
  * auth: `Authorization: Bearer {printhost_apikey}`
  * test: `GET /api/v1/version/`
  * status: `GET /api/v1/printers/`; Mobile Slicer reports the configured
    printer id when it is present, or the first listed printer when no id is set
  * upload: multipart `POST /api/v1/g_code_files/` with fields `print`, `path`,
    `printer_id`, `filename`, and file field `file`
  * Mobile Slicer currently requires users to paste the Obico printer id into
    `Printer path or port`; Orca can enumerate printers into a picker, so an
    in-app picker remains future work.
* SimplyPrint:
  * implemented from Orca's `SimplyPrint.cpp`, bounded to the portions that fit
    Mobile Slicer's current Android profile and browser handoff model
  * Orca's fixed URLs:
    * panel/home: `https://simplyprint.io`
    * API: `https://api.simplyprint.io`
    * temp upload: `https://simplyprint.io/api/files/TempUpload`
  * Orca's OAuth details:
    * client id: `simplyprintorcaslicer`
    * callback: `http://localhost:21328/callback`
    * scopes: `user.read files.temp_upload`
    * token URL: `https://api.simplyprint.io/oauth2/Token`
  * Mobile Slicer runs the same OAuth/PKCE login shape from Android:
    * opens Orca's SimplyPrint authorize URL in the browser
    * listens for the `http://localhost:21328/callback` redirect while login is
      in progress
    * exchanges the returned code at Orca's token URL
    * stores the access token in `API Key / Password`
    * stores the refresh token in `Password`
    * both token fields are covered by Mobile Slicer's encrypted printer
      credential store and are stripped from normal profile JSON persistence
  * test/status: `GET https://api.simplyprint.io/oauth2/TokenInfo` with
    `Authorization: Bearer {token}` and `User-Agent: SimplyPrint Orca Plugin`
  * token refresh: when the saved access token is missing/expired and a refresh
    token is available, Mobile Slicer posts `grant_type=refresh_token`,
    `client_id=simplyprintorcaslicer`, and `refresh_token` to Orca's token URL
    before retrying the API operation
  * upload-only / upload-to-queue: multipart
    `POST https://simplyprint.io/api/files/TempUpload` with file field `file`
  * successful upload requires a returned `uuid`; Mobile Slicer opens the
    matching panel import URL in the in-app browser:
    `https://simplyprint.io/panel?import=tmp:{uuid}&filename={filename}`
  * files over 100 MB follow Orca's chunked `ChunkReceive` flow:
    * first chunk posts `i=0`, `temp=true`, `filename`, `chunks`, and
      `totalsize`
    * the first response must include `id` and `delete_token`
    * later chunks post `i`, `temp=true`, and `id`
    * if a chunk fails, Mobile Slicer calls Orca's cleanup URL with `id`,
      `temp=true`, and `delete`
    * finalization posts `chunkId` to
      `https://simplyprint.io/api/files/TempUpload`
  * Orca opens the external browser or device panel after temp upload. Mobile
    Slicer follows the same handoff with its in-app printer browser.
* Duet classic RepRapFirmware:
  * test/connect: `GET /rr_connect?password={password}&time=...`
  * upload: `POST /rr_upload?name=0:/gcodes/{filename}&time=...` with the raw
    G-code file as the body
  * start: `GET /rr_gcode?gcode=M32%20%220:/gcodes/{filename}%22`
  * disconnect: `GET /rr_disconnect`
  * Mobile Slicer uses `printhost_apikey` as the Duet password and falls back
    to Orca's default `reprap` password when it is blank.
* Duet DSF:
  * test/connect: `GET /machine/status`
  * upload: `PUT /machine/file/gcodes/{filename}` with the raw G-code file as
    the body
  * start: `POST /machine/code` with `M32 "0:/gcodes/{filename}"`.
* Repetier and Obico expose multiple-printer selection in Orca's dialog.
* Repetier:
  * test: `GET /printer/info` with `X-Api-Key`
  * upload-only: multipart `POST /printer/model/{slug}` with `a=upload` and
    file field `filename`
  * upload-and-start: multipart `POST /printer/job/{slug}` with `a=upload`,
    `name={filename}`, `autostart=true`, and file field `filename`
  * status: `GET /printer/list` for basic printer state, plus
    `GET /printer/api/{slug}?a=stateList&includeHistory=false` for basic
    temperature/layer detail when available
  * printer picker: `GET /printer/list`, storing selected `slug` in
    `Printer path or port`
  * group picker: `GET /printer/api/{slug}?a=listModelGroups`, storing the
    selected group in `printhost_group`; upload-only sends non-default groups
    as Orca's `group` form field
* ESP3D:
  * test/status: `GET /command?plain=M105`
  * upload: multipart `POST /upload_serial` with file field `file`
  * upload-and-start: after upload, wait briefly, then send
    `GET /command?plain=M23 {8.3 filename}` and `GET /command?plain=M24`
  * Mobile Slicer mirrors Orca's 8.3 filename constraint to avoid ESP3D firmware
    issues with long file names.
* CrealityPrint:
  * test/status: `GET /info` with `Authorization: Bearer {printhost_apikey}`
    when a token is configured
  * upload: multipart `POST /upload/{safe_filename}` with form field `path`
    and file field `file`; Orca's safe filename behavior replaces spaces with
    underscores for the upload URL and start command
  * upload-and-start: open a plain WebSocket to `{host}:9999/` and send
    `{"method":"set","params":{"opGcodeFile":"printprt:/usr/data/printer_data/gcodes/{safe_filename}"}}`
  * Mobile Slicer follows Orca's HTTP and WebSocket shape, but this path still
    needs real Creality device validation.
* Elegoo Link:
  * test: Orca first tries OctoPrint compatibility, then native Elegoo Link by
    requesting the base URL and checking for `ELEGOO` in the response body
  * upload: native Elegoo Link uses multipart `POST /uploadFile/upload` in
    1 MiB chunks with fields `Check=1`, `S-File-MD5`, `Offset`, `Uuid`,
    `TotalSize`, and file field `File`
  * upload response: each chunk must return JSON with `code == "000000"`;
    `messages` are surfaced when present
  * upload-and-start: open a plain WebSocket to `{host}:3030/websocket`, send
    SDCP status command `0` until file-check status `8` clears, then send SDCP
    start command `128` with `Filename=/local/{filename}`, `StartLayer=0`,
    `Calibration_switch=0`, `PrintPlatformType=0`, and `Tlp_Switch=0`
  * Mobile Slicer follows Orca's native upload/start shape, but without an
    Elegoo printer available this remains hardware-unverified.
* FlashAir:
  * test/status: `GET /command.cgi?op=118`; response `1` means upload is enabled
  * prepare upload: `GET /upload.cgi?WRITEPROTECT=ON&FTIME={fat timestamp}`
  * set upload directory: `GET /upload.cgi?UPDIR={directory}`
  * upload: multipart `POST /upload.cgi` with file field `file`; response body
    must contain `SUCCESS`
  * Mobile Slicer uses `Printer path or port` as the optional upload directory,
    defaulting to `/`.
  * FlashAir upload can modify the SD card while the printer host also has the
    card mounted; this follows Orca and the official docs, but hardware
    validation should confirm the target printer notices the new file.
* AstroBox:
  * test/status: `GET /api/version` with `X-Api-Key` when configured
  * upload: multipart `POST /api/files/local` with fields `print`, `path`, and
    file field `file`
  * upload-and-start: same upload endpoint with `print=true`
  * Mobile Slicer uses `Printer path or port` as the optional AstroBox upload
    path, matching Orca's upload-path parent field concept.
* MKS:
  * test/status: send `M105` to the TCP console
  * console port: Orca hard-codes `8080`; Mobile Slicer defaults to `8080` and
    lets `Printer path or port` override it for uncommon firmware setups
  * upload: raw HTTP `POST /upload?X-Filename={filename}` with the G-code file
    as the request body
  * upload response: JSON `err` defaults to `0`; any non-zero `err` is treated
    as a failed send
  * upload-and-start: wait briefly after upload, then send `M23 {filename}` and
    `M24` over the TCP console, matching Orca's firmware-delay workaround
* Flashforge:
  * test/status: send `~M601 S1` to the TCP console
  * console port: Orca hard-codes `8899`; Mobile Slicer defaults to `8899` and
    lets `Printer path or port` override it for uncommon firmware setups
  * setup before upload: `~M601 S1`, `~M115`, `~M640` for Klipper G-code
    flavor or `~M650` otherwise, then `~M119`
  * upload: send `~M28 {byte_size} 0:/user/{filename}`, wait for `ok`, then
    stream the G-code file in 4 KiB chunks
  * save: wait briefly after upload, then send `~M29`
  * upload-and-start: send `~M23 0:/user/{filename}`
  * Mobile Slicer mirrors Orca's command order, but does not yet parse detailed
    Flashforge status responses beyond basic online/offline reporting.
* SimplyPrint uses a cloud login/upload flow and can require chunked upload for
  large files. Mobile Slicer's current pass supports OAuth-backed small-file
  upload only.
* Qidi is represented as a `printer_agent` named `qidi` and extends Orca's
  Moonraker printer-agent path; the first Qidi implementation should be based
  on that source path, not on the `Octo/Klipper` label alone.

Likely Android protocol tasks:

* normalize host input with or without scheme.
* support HTTP on local printer IPs intentionally and visibly.
* support HTTPS with normal Android trust first.
* decide whether custom CA files are phase-one or phase-two.
* keep auth headers/body logic host-specific.
* avoid logging API keys, passwords, URLs containing credentials, or bearer
  tokens.

## Discovery

Orca's `Browse` button depends on host auto-discovery support. Android should
treat discovery as phase two.

Likely Android discovery work:

* Android NSD/mDNS for local OctoPrint/Moonraker-style services.
* host-specific discovery adapters where Orca has them.
* manual IP entry always remains available.
* discovery must never be required for upload.

## Security Requirements

Before runtime connection support ships:

* add Android `INTERNET` permission if it is not already present. Done.
* store API keys/passwords in encrypted Android storage instead of plain
  profile JSON or ordinary preferences. Done with Android Keystore-backed
  encrypted preferences for API keys, usernames, and passwords.
* decide how exported profiles handle secrets:
  * safest default: do not export secrets unless user explicitly includes them.
  * imported profiles may include connection fields, but the UI should make
    stored secrets visible/editable only through password/API-key controls.
    Local profile and saved-project JSON persistence now omits secrets by
    default; an explicit include-secrets export option is future work if needed.
* redact secrets in logs, crash reports, docs, and test fixtures. Done for
  printer API/WebView user-facing URL messages and native slice config output.
* show clear error messages for timeout, DNS/parse failure, refused connection,
  TLS failure, auth failure, and incompatible host response. Partially done;
  auth and incompatible-host responses still need clearer host-specific copy.

## Product Behavior

Current and expected user-facing behavior:

* `Connection` tab edits the profile connection.
* `Test Connection` validates the selected host.
* Slice/export remains possible without any printer connection.
* Printer controls appear on the Preview tab after a successful slice; missing
  connection settings show a setup prompt instead of hiding the buttons.
* `Printer` opens the configured web UI or host inside Mobile Slicer.
* Uploads show progress and can be cancelled.
* Failed uploads keep the local G-code file available.
* Upload-and-start sends the host's standard multipart `print=true` request.
* Unsupported host types should show an honest "configuration only" message
  instead of failing as if the printer were broken.

## Documentation Rules

When printer connection implementation begins:

* update this file in the same run as each host-client change.
* update `README/SETTINGS_CHECKLIST.md` when field visibility or profile
  coverage changes.
* add host support status:
  * `Config only`
  * `Test implemented`
  * `Upload implemented`
  * `Device tested`
* include the exact Orca source file/function used as the behavior reference.

# Printer WebView Security

The in-app printer browser is for trusted printer UIs and camera streams, not
general web browsing.

Current guardrails:

* top-level navigation is allowed only for the configured printer host, local
  printer/private-network hosts, `about:` pages, and `data:` pages
* top-level navigation to unrelated external sites is blocked
* SSL certificate errors are always cancelled
* JavaScript stays enabled because printer UIs commonly require it
* file and content access are disabled
* mixed content is blocked for HTTPS pages
* camera permission is granted only when the request comes from a trusted
  printer origin and Android runtime camera permission is already granted

If a printer uses a self-signed HTTPS certificate, prefer one of these paths:

* use the printer's HTTP UI on the local network
* install/trust the certificate at the OS/profile level
* add an explicit per-printer trust workflow before allowing certificate-error
  bypasses in the WebView

Do not call `SslErrorHandler.proceed()` in the embedded browser without a
deliberate, user-visible trust decision.

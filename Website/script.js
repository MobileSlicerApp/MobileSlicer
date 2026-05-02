const navItems = [
  ["home", "Home", "/"],
  ["about", "About", "/about.html"],
  ["features", "Features", "/features.html"],
  ["roadmap", "Roadmap", "/roadmap.html"],
  ["faq", "FAQ", "/faq.html"],
  ["support", "Support", "/support.html"],
  ["beta", "Download the Beta", "/beta.html"],
];

const footerItems = {
  privacy: ["Privacy", "/privacy.html"],
  faq: ["FAQ", "/faq.html"],
  features: ["Features", "/features.html"],
  beta: ["Download the Beta", "/beta.html"],
  home: ["Home", "/"],
};

class SiteHeader extends HTMLElement {
  connectedCallback() {
    const active = this.getAttribute("active") || "";
    const links = navItems
      .map(([key, label, href]) => {
        const className = key === active ? ' class="active"' : "";
        return `<a${className} href="${href}">${label}</a>`;
      })
      .join("");

    this.innerHTML = `
      <header class="site-header">
        <a class="brand" href="/" aria-label="MobileSlicer home">
          <img src="/assets/mobileslicer-logo.svg" alt="">
          <span><strong>MobileSlicer</strong><small>Android-first slicing workflow</small></span>
        </a>
        <nav class="tabs" aria-label="Primary navigation">${links}</nav>
      </header>
    `;
  }
}

class SiteFooter extends HTMLElement {
  connectedCallback() {
    const keys = (this.getAttribute("links") || "privacy,faq,beta")
      .split(",")
      .map((key) => key.trim())
      .filter(Boolean);
    const links = keys
      .map((key) => footerItems[key])
      .filter(Boolean)
      .map(([label, href]) => `<a href="${href}">${label}</a>`)
      .join("");

    this.innerHTML = `
      <footer>
        <span>MobileSlicer</span>
        <nav class="footer-links" aria-label="Footer navigation">${links}</nav>
      </footer>
    `;
  }
}

if (!customElements.get("site-header")) {
  customElements.define("site-header", SiteHeader);
}

if (!customElements.get("site-footer")) {
  customElements.define("site-footer", SiteFooter);
}

const form = document.querySelector("#betaForm");
const submitButton = document.querySelector("#submitButton");
const downloadButton = document.querySelector("#downloadButton");
const formHint = document.querySelector("#formHint");
const feedbackForm = document.querySelector("#feedbackForm");
const feedbackSubmitButton = document.querySelector("#feedbackSubmitButton");
const feedbackHint = document.querySelector("#feedbackHint");
const feedbackPhotos = document.querySelector("#feedbackPhotos");

if (form && submitButton && downloadButton && formHint) {
  function fields() {
    return Array.from(form.elements).filter((element) => element.matches("input, textarea"));
  }

  function formComplete() {
    return fields().every((element) => element.checkValidity() && element.value.trim().length > 0);
  }

  function payload() {
    return Object.fromEntries(fields().map((element) => [element.name, element.value.trim()]));
  }

  function setMessage(message, type = "muted") {
    formHint.textContent = message;
    formHint.dataset.type = type;
  }

  function setDownload(url) {
    downloadButton.href = url;
    downloadButton.hidden = false;
    downloadButton.removeAttribute("aria-disabled");
    downloadButton.classList.remove("disabled");
  }

  function updateSubmitState() {
    const complete = formComplete();
    submitButton.disabled = !complete;
    if (!complete) {
      downloadButton.hidden = true;
      downloadButton.classList.add("disabled");
      downloadButton.setAttribute("aria-disabled", "true");
      setMessage("The download link is available after all fields are entered.");
    }
  }

  form.addEventListener("input", updateSubmitState);
  form.addEventListener("submit", async (event) => {
    event.preventDefault();
    if (!formComplete()) {
      updateSubmitState();
      return;
    }

    submitButton.disabled = true;
    setMessage("Submitting beta info...");

    try {
      const response = await fetch("/api/register", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(payload()),
      });
      const data = await response.json();
      if (!response.ok || !data.downloadUrl) {
        throw new Error(data.error || "Registration failed.");
      }

      setDownload(data.downloadUrl);
      setMessage("Registration captured. Your download link is ready for 20 minutes.", "success");
    } catch (error) {
      submitButton.disabled = false;
      setMessage(error.message || "Registration failed. Please try again.", "error");
    }
  });

  updateSubmitState();
}

if (feedbackForm && feedbackSubmitButton && feedbackHint) {
  function feedbackFields() {
    return Array.from(feedbackForm.elements).filter((element) => element.matches("input:not([type='file']), select, textarea"));
  }

  function feedbackComplete() {
    return feedbackFields().every((element) => element.checkValidity() && element.value.trim().length > 0);
  }

  function setFeedbackMessage(message, type = "muted") {
    feedbackHint.textContent = message;
    feedbackHint.dataset.type = type;
  }

  function updateFeedbackState() {
    const photoCount = feedbackPhotos?.files?.length || 0;
    const tooManyPhotos = photoCount > 4;
    feedbackSubmitButton.disabled = !feedbackComplete() || tooManyPhotos;
    if (tooManyPhotos) {
      setFeedbackMessage("Please attach 4 images or fewer.", "error");
    } else if (!feedbackComplete()) {
      setFeedbackMessage("Feedback type, title, and details are required.");
    }
  }

  feedbackForm.addEventListener("input", updateFeedbackState);
  feedbackForm.addEventListener("change", updateFeedbackState);
  feedbackForm.addEventListener("submit", async (event) => {
    event.preventDefault();
    updateFeedbackState();
    if (feedbackSubmitButton.disabled) return;

    feedbackSubmitButton.disabled = true;
    setFeedbackMessage("Submitting feedback...");

    try {
      const response = await fetch("/api/feedback", {
        method: "POST",
        body: new FormData(feedbackForm),
      });
      const data = await response.json();
      if (!response.ok) {
        throw new Error(data.error || "Feedback submission failed.");
      }

      feedbackForm.reset();
      setFeedbackMessage("Feedback submitted. Thank you for testing MobileSlicer.", "success");
    } catch (error) {
      feedbackSubmitButton.disabled = false;
      setFeedbackMessage(error.message || "Feedback submission failed. Please try again.", "error");
    }
  });

  updateFeedbackState();
}

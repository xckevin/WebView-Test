package com.xckevin.android.app.webview.test.debug

object PageScripts {
    data class ScriptTemplate(
        val name: String,
        val script: String,
    )

    fun readLocalStorage(): String = wrap(
        """
        const entries = {};
        for (let index = 0; index < localStorage.length; index += 1) {
          const key = localStorage.key(index);
          entries[key] = localStorage.getItem(key);
        }
        return entries;
        """.trimIndent()
    )

    fun clearLocalStorage(): String = wrap(
        """
        localStorage.clear();
        return "localStorage cleared";
        """.trimIndent()
    )

    fun readSessionStorage(): String = wrap(
        """
        const entries = {};
        for (let index = 0; index < sessionStorage.length; index += 1) {
          const key = sessionStorage.key(index);
          entries[key] = sessionStorage.getItem(key);
        }
        return entries;
        """.trimIndent()
    )

    fun clearSessionStorage(): String = wrap(
        """
        sessionStorage.clear();
        return "sessionStorage cleared";
        """.trimIndent()
    )

    fun deleteStorageKey(storageName: String, key: String): String {
        val safeStorageName = when (storageName) {
            "localStorage" -> "localStorage"
            "sessionStorage" -> "sessionStorage"
            else -> "localStorage"
        }
        val encodedKey = key.toJsonStringLiteral()
        return wrap(
            """
            ${safeStorageName}.removeItem($encodedKey);
            return "${safeStorageName} key removed: ${key.toResultMessage()}";
            """.trimIndent()
        )
    }

    fun writeStorageKey(storageName: String, key: String, value: String): String {
        val safeStorageName = when (storageName) {
            "localStorage" -> "localStorage"
            "sessionStorage" -> "sessionStorage"
            else -> "localStorage"
        }
        val encodedKey = key.toJsonStringLiteral()
        val encodedValue = value.toJsonStringLiteral()
        return wrap(
            """
            ${safeStorageName}.setItem($encodedKey, $encodedValue);
            return "${safeStorageName} key saved: ${key.toResultMessage()}";
            """.trimIndent()
        )
    }

    fun writeCookie(name: String, value: String): String {
        val encodedName = name.toJsonStringLiteral()
        val encodedValue = value.toJsonStringLiteral()
        return wrap(
            """
            document.cookie = ${encodedName} + "=" + encodeURIComponent(${encodedValue}) + "; path=/";
            return "cookie saved: ${name.toResultMessage()}";
            """.trimIndent()
        )
    }

    fun deleteCookie(name: String): String {
        val encodedName = name.toJsonStringLiteral()
        return wrap(
            """
            document.cookie = ${encodedName} + "=; expires=Thu, 01 Jan 1970 00:00:00 GMT; path=/";
            return "cookie deleted: ${name.toResultMessage()}";
            """.trimIndent()
        )
    }

    fun readCookiesStructured(): String = wrap(
        """
        return document.cookie
          ? document.cookie.split(";").map((entry) => {
              const separator = entry.indexOf("=");
              const rawName = separator >= 0 ? entry.slice(0, separator) : entry;
              const rawValue = separator >= 0 ? entry.slice(separator + 1) : "";
              return {
                name: rawName.trim(),
                value: rawValue.trim(),
                length: rawValue.trim().length
              };
            })
          : [];
        """.trimIndent()
    )

    fun readSource(): String = wrap(
        """
        return document.documentElement.outerHTML;
        """.trimIndent()
    )

    fun readElementsSummary(search: String = "", selector: String = ""): String {
        val querySelector = selector.ifBlank {
            "a, button, input, textarea, select, form, img, video, iframe"
        }.toJsonStringLiteral()
        val searchText = search.toJsonStringLiteral()
        return wrap(
        """
        const selector = $querySelector;
        const search = $searchText.trim().toLowerCase();
        const matchesSearch = (summary) => {
          if (!search) return true;
          return [
            summary.tag,
            summary.id,
            summary.name,
            summary.type,
            summary.text,
            summary.href,
            summary.src,
            summary.className
          ].some((value) => String(value || "").toLowerCase().includes(search));
        };
        return Array.from(document.querySelectorAll(selector))
          .slice(0, 250)
          .map((element) => ({
            tag: element.tagName.toLowerCase(),
            id: element.id || "",
            className: element.className || "",
            name: element.getAttribute("name") || "",
            type: element.getAttribute("type") || "",
            text: (element.innerText || element.alt || element.value || "").trim().slice(0, 120),
            href: element.href || "",
            src: element.src || "",
            visible: !!(element.offsetWidth || element.offsetHeight || element.getClientRects().length)
          }))
          .filter(matchesSearch);
        """.trimIndent()
        )
    }

    fun readElementDetails(selector: String): String {
        val querySelector = selector.ifBlank { "body" }.toJsonStringLiteral()
        return wrap(
            """
            return Array.from(document.querySelectorAll($querySelector))
              .slice(0, 100)
              .map((element) => {
                const rect = element.getBoundingClientRect();
                const attributes = {};
                Array.from(element.attributes || []).forEach((attribute) => {
                  attributes[attribute.name] = attribute.value;
                });
                return {
                  tag: element.tagName.toLowerCase(),
                  id: element.id || "",
                  className: element.className || "",
                  text: (element.innerText || element.alt || element.value || "").trim().slice(0, 300),
                  visible: !!(element.offsetWidth || element.offsetHeight || element.getClientRects().length),
                  rect: {
                    x: Math.round(rect.x),
                    y: Math.round(rect.y),
                    width: Math.round(rect.width),
                    height: Math.round(rect.height)
                  },
                  attributes: attributes,
                  href: element.href || "",
                  src: element.src || ""
                };
              });
            """.trimIndent()
        )
    }

    fun startFloatingInspectPointer(): String = wrap(
        """
        const existing = window.__wvDebugInspect;
        if (existing && typeof existing.cleanup === "function") {
          existing.cleanup();
        }

        const state = {
          target: null,
          x: Math.max(24, Math.round(window.innerWidth / 2) - 24),
          y: Math.max(24, Math.round(window.innerHeight / 2) - 24)
        };
        const overlay = document.createElement("div");
        const label = document.createElement("div");
        const highlight = document.createElement("div");
        const crosshair = document.createElement("div");
        const actionBar = document.createElement("div");
        const confirmButton = document.createElement("button");
        const cancelButton = document.createElement("button");

        overlay.id = "__wv_debug_inspect_pointer";
        overlay.setAttribute("aria-label", "WebView inspect pointer");
        overlay.style.cssText = [
          "position:fixed",
          "left:" + state.x + "px",
          "top:" + state.y + "px",
          "width:52px",
          "height:52px",
          "border-radius:999px",
          "background:rgba(20,22,26,0.88)",
          "border:2px solid #7dd3fc",
          "box-shadow:0 8px 24px rgba(0,0,0,0.34)",
          "z-index:2147483647",
          "cursor:grab",
          "touch-action:none",
          "box-sizing:border-box"
        ].join(";");
        crosshair.style.cssText = [
          "position:absolute",
          "left:50%",
          "top:50%",
          "width:18px",
          "height:18px",
          "margin-left:-9px",
          "margin-top:-9px",
          "border:2px solid #ffffff",
          "border-radius:999px",
          "box-sizing:border-box",
          "pointer-events:none"
        ].join(";");
        label.style.cssText = [
          "position:absolute",
          "left:50%",
          "top:58px",
          "transform:translateX(-50%)",
          "max-width:240px",
          "padding:5px 8px",
          "border-radius:6px",
          "background:rgba(20,22,26,0.92)",
          "color:#ffffff",
          "font:12px/1.3 -apple-system,BlinkMacSystemFont,Segoe UI,sans-serif",
          "white-space:nowrap",
          "overflow:hidden",
          "text-overflow:ellipsis",
          "pointer-events:none"
        ].join(";");
        highlight.id = "__wv_debug_inspect_highlight";
        highlight.style.cssText = [
          "position:fixed",
          "display:none",
          "border:2px solid #38bdf8",
          "background:rgba(56,189,248,0.12)",
          "z-index:2147483646",
          "pointer-events:none",
          "box-sizing:border-box"
        ].join(";");
        actionBar.id = "__wv_debug_inspect_actions";
        actionBar.style.cssText = [
          "position:fixed",
          "right:calc(env(safe-area-inset-right, 0px) + 12px)",
          "top:calc(env(safe-area-inset-top, 0px) + 12px)",
          "display:flex",
          "gap:8px",
          "padding:8px",
          "border-radius:10px",
          "background:rgba(20,22,26,0.92)",
          "box-shadow:0 8px 24px rgba(0,0,0,0.34)",
          "z-index:2147483647",
          "pointer-events:auto",
          "max-width:calc(100vw - 24px)",
          "box-sizing:border-box"
        ].join(";");
        const buttonStyle = [
          "min-width:88px",
          "height:40px",
          "border:0",
          "border-radius:7px",
          "padding:0 12px",
          "font:600 13px/40px -apple-system,BlinkMacSystemFont,Segoe UI,sans-serif",
          "color:#0f172a",
          "background:#7dd3fc",
          "touch-action:manipulation"
        ].join(";");
        confirmButton.type = "button";
        confirmButton.textContent = "Confirm";
        confirmButton.style.cssText = buttonStyle;
        cancelButton.type = "button";
        cancelButton.textContent = "Cancel";
        cancelButton.style.cssText = buttonStyle + ";background:#e5e7eb";

        const labelFor = (element) => {
          if (!element || !element.tagName) return "No element";
          const id = element.id ? "#" + element.id : "";
          const className = (element.getAttribute("class") || "")
            .trim()
            .split(/\s+/)
            .filter(Boolean)
            .slice(0, 3)
            .map((value) => "." + value)
            .join("");
          return element.tagName.toLowerCase() + id + className;
        };
        const findTarget = () => {
          const centerX = state.x + 26;
          const centerY = state.y + 26;
          overlay.style.pointerEvents = "none";
          actionBar.style.pointerEvents = "none";
          const target = document.elementFromPoint(centerX, centerY);
          overlay.style.pointerEvents = "auto";
          actionBar.style.pointerEvents = "auto";
          state.target = target === overlay || target === label || target === crosshair || target === actionBar ? null : target;
          label.textContent = labelFor(state.target);
          if (state.target && state.target.getBoundingClientRect) {
            const rect = state.target.getBoundingClientRect();
            highlight.style.display = "block";
            highlight.style.left = Math.round(rect.left) + "px";
            highlight.style.top = Math.round(rect.top) + "px";
            highlight.style.width = Math.round(rect.width) + "px";
            highlight.style.height = Math.round(rect.height) + "px";
          } else {
            highlight.style.display = "none";
          }
        };
        const moveTo = (x, y) => {
          state.x = Math.min(Math.max(0, Math.round(x - 26)), Math.max(0, window.innerWidth - 52));
          state.y = Math.min(Math.max(0, Math.round(y - 26)), Math.max(0, window.innerHeight - 52));
          overlay.style.left = state.x + "px";
          overlay.style.top = state.y + "px";
          findTarget();
        };
        const isInspectorNode = (node) => {
          return node === overlay ||
            node === label ||
            node === crosshair ||
            node === actionBar ||
            node === confirmButton ||
            node === cancelButton ||
            (node && node.closest && node.closest("#__wv_debug_inspect_actions"));
        };
        const stopPageEvent = (event) => {
          event.preventDefault();
          event.stopPropagation();
        };
        let dragging = false;
        overlay.addEventListener("pointerdown", (event) => {
          dragging = true;
          overlay.style.cursor = "grabbing";
          overlay.setPointerCapture(event.pointerId);
          stopPageEvent(event);
        });
        overlay.addEventListener("pointermove", (event) => {
          if (dragging) moveTo(event.clientX, event.clientY);
        });
        overlay.addEventListener("pointerup", (event) => {
          dragging = false;
          overlay.style.cursor = "grab";
          try { overlay.releasePointerCapture(event.pointerId); } catch (_) {}
          findTarget();
        });
        overlay.addEventListener("pointercancel", () => {
          dragging = false;
          overlay.style.cursor = "grab";
        });
        const pagePointerDown = (event) => {
          if (isInspectorNode(event.target)) return;
          moveTo(event.clientX, event.clientY);
          stopPageEvent(event);
        };
        const pageClick = (event) => {
          if (isInspectorNode(event.target)) return;
          stopPageEvent(event);
        };

        overlay.appendChild(crosshair);
        overlay.appendChild(label);
        actionBar.appendChild(confirmButton);
        actionBar.appendChild(cancelButton);
        document.documentElement.appendChild(highlight);
        document.documentElement.appendChild(overlay);
        document.documentElement.appendChild(actionBar);
        document.addEventListener("pointerdown", pagePointerDown, true);
        document.addEventListener("click", pageClick, true);

        window.__wvDebugInspect = {
          overlay: overlay,
          highlight: highlight,
          get target() { return state.target; },
          cleanup: function() {
            document.removeEventListener("pointerdown", pagePointerDown, true);
            document.removeEventListener("click", pageClick, true);
            if (overlay.parentNode) overlay.parentNode.removeChild(overlay);
            if (highlight.parentNode) highlight.parentNode.removeChild(highlight);
            if (actionBar.parentNode) actionBar.parentNode.removeChild(actionBar);
            if (window.__wvDebugInspect === this) delete window.__wvDebugInspect;
          },
          confirm: function() {
            const payload = serializeSelectedInspectTree(state.target, state.x + 26, state.y + 26);
            this.cleanup();
            return payload;
          }
        };
        confirmButton.addEventListener("click", (event) => {
          stopPageEvent(event);
          const payload = window.__wvDebugInspect.confirm();
          window.__wvDebugInspectLastResult = payload;
          if (window.__wvAndroidDebug && typeof window.__wvAndroidDebug.onInspectResult === "function") {
            window.__wvAndroidDebug.onInspectResult(JSON.stringify(payload));
          } else {
            window.__wvDebugInspectLastResult = payload;
          }
        });
        cancelButton.addEventListener("click", (event) => {
          stopPageEvent(event);
          if (window.__wvDebugInspect && typeof window.__wvDebugInspect.cleanup === "function") {
            window.__wvDebugInspect.cleanup();
          }
        });

        function serializeSelectedInspectTree(target, pointX, pointY) {
          const MAX_PATH_DEPTH = 24;
          const MAX_ATTRIBUTES = 16;
          const MAX_TEXT_LENGTH = 500;
          const MAX_ATTRIBUTE_LENGTH = 160;
          const elements = [];
          let node = target && target.nodeType === 1 ? target : null;
          while (node && node.nodeType === 1) {
            elements.unshift(node);
            node = node.parentElement;
          }
          const truncated = elements.length > MAX_PATH_DEPTH;
          const path = elements.slice(Math.max(0, elements.length - MAX_PATH_DEPTH));
          const summarize = (element, depth) => {
            const rect = element.getBoundingClientRect();
            const attributes = {};
            Array.from(element.attributes || []).slice(0, MAX_ATTRIBUTES).forEach((attribute) => {
              attributes[attribute.name] = String(attribute.value || "").slice(0, MAX_ATTRIBUTE_LENGTH);
            });
            const parent = element.parentElement;
            return {
              tag: element.tagName.toLowerCase(),
              id: element.id || "",
              className: element.getAttribute("class") || "",
              text: (element.innerText || element.alt || element.value || element.textContent || "").trim().slice(0, MAX_TEXT_LENGTH),
              visible: !!(element.offsetWidth || element.offsetHeight || element.getClientRects().length),
              rect: {
                x: Math.round(rect.x),
                y: Math.round(rect.y),
                width: Math.round(rect.width),
                height: Math.round(rect.height)
              },
              attributes: attributes,
              href: typeof element.href === "string" ? element.href : "",
              src: typeof element.src === "string" ? element.src : "",
              depth: depth,
              childIndex: parent ? Array.prototype.indexOf.call(parent.children, element) : 0,
              childCount: element.children ? element.children.length : 0
            };
          };
          return {
            type: "selectedElementTree",
            selectedIndex: Math.max(0, path.length - 1),
            truncatedAncestors: truncated,
            point: { x: Math.round(pointX), y: Math.round(pointY) },
            path: path.map((element, index) => summarize(element, index))
          };
        }

        moveTo(state.x + 26, state.y + 26);
        return "Inspect pointer started. Tap or drag on the page, then use Confirm or Cancel on the page overlay.";
        """.trimIndent()
    )

    fun readLastFloatingInspectPointerResult(): String = wrap(
        """
        const result = window.__wvDebugInspectLastResult || null;
        if (result) {
          delete window.__wvDebugInspectLastResult;
        }
        return result;
        """.trimIndent()
    )

    fun clearLastFloatingInspectPointerResult(): String = wrap(
        """
        delete window.__wvDebugInspectLastResult;
        return "Inspect pointer result cleared.";
        """.trimIndent()
    )

    fun confirmFloatingInspectPointer(): String = wrap(
        """
        if (!window.__wvDebugInspect || typeof window.__wvDebugInspect.confirm !== "function") {
          return { error: "Inspect pointer is not active." };
        }
        return window.__wvDebugInspect.confirm();
        """.trimIndent()
    )

    fun cancelFloatingInspectPointer(): String = wrap(
        """
        if (window.__wvDebugInspect && typeof window.__wvDebugInspect.cleanup === "function") {
          window.__wvDebugInspect.cleanup();
          return "Inspect pointer cancelled.";
        }
        return "Inspect pointer was not active.";
        """.trimIndent()
    )

    fun installNetworkApiCapture(): String = wrap(
        """
        if (window.__wvDebugNetworkApi && window.__wvDebugNetworkApi.installed) {
          return "Network API capture already installed: " + window.__wvDebugNetworkApi.captures.length + " captured";
        }

        const MAX_BODY_CHARS = 65536;
        const MAX_ENTRIES = 100;
        const originalFetch = window.fetch;
        const originalXhrOpen = XMLHttpRequest.prototype.open;
        const originalXhrSend = XMLHttpRequest.prototype.send;
        const captures = [];

        const textExtensions = [".json", ".txt", ".text", ".html", ".htm", ".css", ".js", ".mjs", ".xml"];
        const isTextLike = (contentType, url) => {
          const type = String(contentType || "").split(";")[0].trim().toLowerCase();
          const path = String(url || "").split("?")[0].split("#")[0].toLowerCase();
          return type.startsWith("text/") ||
            type === "application/json" ||
            type === "text/json" ||
            type.endsWith("+json") ||
            type.includes("javascript") ||
            type.includes("xml") ||
            textExtensions.some((extension) => path.endsWith(extension));
        };
        const capBody = (body) => {
          const text = String(body || "");
          return {
            body: text.length > MAX_BODY_CHARS ? text.slice(0, MAX_BODY_CHARS) : text,
            truncated: text.length > MAX_BODY_CHARS
          };
        };
        const addCapture = (capture) => {
          captures.push(capture);
          while (captures.length > MAX_ENTRIES) captures.shift();
          if (window.__wvAndroidDebug && typeof window.__wvAndroidDebug.onNetworkApiCapture === "function") {
            window.__wvAndroidDebug.onNetworkApiCapture(JSON.stringify(capture));
          }
        };
        const headersToObject = (headers) => {
          const result = {};
          if (!headers) return result;
          if (typeof headers.forEach === "function") {
            headers.forEach((value, key) => { result[key] = value; });
          }
          return result;
        };

        if (typeof originalFetch === "function") {
          window.fetch = function(input, init) {
            const startedAt = Date.now();
            const url = typeof input === "string" ? input : (input && input.url) || "";
            const method = ((init && init.method) || (input && input.method) || "GET").toUpperCase();
            return originalFetch.apply(this, arguments).then((response) => {
              const headers = headersToObject(response.headers);
              const contentType = response.headers && response.headers.get ? response.headers.get("content-type") : "";
              const base = {
                source: "fetch",
                url: response.url || url,
                method: method,
                status: response.status,
                statusText: response.statusText || "",
                responseHeaders: headers,
                contentType: contentType || "",
                timestamp: Date.now(),
                durationMs: Date.now() - startedAt
              };
              if (!isTextLike(contentType, base.url)) {
                addCapture(Object.assign(base, {
                  responseBody: "",
                  bodyTruncated: false,
                  skippedBodyReason: "Non-text response"
                }));
                return response;
              }
              response.clone().text()
                .then((body) => {
                  const capped = capBody(body);
                  addCapture(Object.assign(base, {
                    responseBody: capped.body,
                    bodyTruncated: capped.truncated
                  }));
                })
                .catch((error) => {
                  addCapture(Object.assign(base, {
                    responseBody: "",
                    bodyTruncated: false,
                    skippedBodyReason: String(error)
                  }));
                });
              return response;
            });
          };
        }

        XMLHttpRequest.prototype.open = function(method, url) {
          this.__wvDebugApi = {
            source: "xhr",
            method: String(method || "GET").toUpperCase(),
            url: String(url || ""),
            startedAt: Date.now()
          };
          return originalXhrOpen.apply(this, arguments);
        };
        XMLHttpRequest.prototype.send = function() {
          const xhr = this;
          const meta = xhr.__wvDebugApi || { source: "xhr", method: "GET", url: "", startedAt: Date.now() };
          xhr.addEventListener("loadend", function() {
            const contentType = xhr.getResponseHeader("content-type") || "";
            const headersText = xhr.getAllResponseHeaders ? xhr.getAllResponseHeaders() : "";
            const headers = {};
            headersText.trim().split(/[\r\n]+/).filter(Boolean).forEach((line) => {
              const separator = line.indexOf(":");
              if (separator > 0) headers[line.slice(0, separator).trim()] = line.slice(separator + 1).trim();
            });
            const base = {
              source: "xhr",
              url: xhr.responseURL || meta.url,
              method: meta.method,
              status: xhr.status,
              statusText: xhr.statusText || "",
              responseHeaders: headers,
              contentType: contentType,
              timestamp: Date.now(),
              durationMs: Date.now() - meta.startedAt
            };
            if (!isTextLike(contentType, base.url) || (xhr.responseType && xhr.responseType !== "text")) {
              addCapture(Object.assign(base, {
                responseBody: "",
                bodyTruncated: false,
                skippedBodyReason: "Non-text response"
              }));
              return;
            }
            try {
              const capped = capBody(xhr.responseText || "");
              addCapture(Object.assign(base, {
                responseBody: capped.body,
                bodyTruncated: capped.truncated
              }));
            } catch (error) {
              addCapture(Object.assign(base, {
                responseBody: "",
                bodyTruncated: false,
                skippedBodyReason: String(error)
              }));
            }
          });
          return originalXhrSend.apply(this, arguments);
        };

        window.__wvDebugNetworkApi = {
          installed: true,
          captures: captures,
          clear: function() { captures.length = 0; }
        };
        return "Network API capture installed";
        """.trimIndent()
    )

    fun readNetworkApiCaptures(): String = wrap(
        """
        return window.__wvDebugNetworkApi && window.__wvDebugNetworkApi.captures
          ? window.__wvDebugNetworkApi.captures.slice()
          : [];
        """.trimIndent()
    )

    fun clearNetworkApiCaptures(): String = wrap(
        """
        if (window.__wvDebugNetworkApi && typeof window.__wvDebugNetworkApi.clear === "function") {
          window.__wvDebugNetworkApi.clear();
          return "Network API captures cleared";
        }
        return "Network API capture is not installed.";
        """.trimIndent()
    )

    fun templates(): List<ScriptTemplate> =
        listOf(
            ScriptTemplate(
                name = "Page info",
                script = """
                    return {
                      href: location.href,
                      title: document.title,
                      readyState: document.readyState,
                      charset: document.characterSet,
                      referrer: document.referrer
                    };
                """.trimIndent(),
            ),
            ScriptTemplate(
                name = "Viewport",
                script = """
                    return {
                      innerWidth: window.innerWidth,
                      innerHeight: window.innerHeight,
                      devicePixelRatio: window.devicePixelRatio,
                      visualViewport: window.visualViewport ? {
                        width: Math.round(window.visualViewport.width),
                        height: Math.round(window.visualViewport.height),
                        scale: window.visualViewport.scale
                      } : null
                    };
                """.trimIndent(),
            ),
            ScriptTemplate(
                name = "List links",
                script = """
                    return Array.from(document.links).slice(0, 100).map((link) => ({
                      text: (link.innerText || link.title || "").trim().slice(0, 120),
                      href: link.href
                    }));
                """.trimIndent(),
            ),
            ScriptTemplate(
                name = "List forms",
                script = """
                    return Array.from(document.forms).map((form) => ({
                      action: form.action,
                      method: form.method,
                      fields: Array.from(form.elements).map((field) => ({
                        tag: field.tagName.toLowerCase(),
                        name: field.name || "",
                        type: field.type || "",
                        id: field.id || ""
                      }))
                    }));
                """.trimIndent(),
            ),
            ScriptTemplate(
                name = "Images",
                script = """
                    return Array.from(document.images).slice(0, 100).map((image) => ({
                      src: image.currentSrc || image.src,
                      alt: image.alt || "",
                      width: image.naturalWidth,
                      height: image.naturalHeight,
                      complete: image.complete
                    }));
                """.trimIndent(),
            ),
        )


    fun executeUserScript(script: String): String {
        val escapedScript = script
            .replace("</script", "<\\/script")
        return """
        (function(){
          try {
            return JSON.stringify((function(){
              $escapedScript
            })());
          } catch(e) {
            return JSON.stringify({error:String(e)});
          }
        })();
        """.trimIndent()
    }

    private fun wrap(body: String): String {
        return """
        (function() {
          try {
            const __debugResult = (function() {
        ${body.prependIndent("      ")}
            })();
            return JSON.stringify({ ok: true, value: __debugResult === undefined ? null : __debugResult });
          } catch (error) {
            return JSON.stringify({
              ok: false,
              error: String(error && error.stack ? error.stack : error)
            });
          }
        })();
        """.trimIndent()
    }

    private fun String.toJsonStringLiteral(): String =
        buildString {
            append('"')
            this@toJsonStringLiteral.forEach { char ->
                when (char) {
                    '\\' -> append("\\\\")
                    '"' -> append("\\\"")
                    '\n' -> append("\\n")
                    '\r' -> append("\\r")
                    '\t' -> append("\\t")
                    else -> append(char)
                }
            }
            append('"')
        }

    private fun String.toResultMessage(): String =
        replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
}

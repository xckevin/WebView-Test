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

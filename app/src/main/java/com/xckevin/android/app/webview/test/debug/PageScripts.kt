package com.xckevin.android.app.webview.test.debug

object PageScripts {
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

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

    fun readSource(): String = wrap(
        """
        return document.documentElement.outerHTML;
        """.trimIndent()
    )

    fun readElementsSummary(): String = wrap(
        """
        return Array.from(document.querySelectorAll("a, button, input, textarea, select, form, img, video, iframe"))
          .slice(0, 250)
          .map((element) => ({
            tag: element.tagName.toLowerCase(),
            id: element.id || "",
            name: element.getAttribute("name") || "",
            type: element.getAttribute("type") || "",
            text: (element.innerText || element.alt || element.value || "").trim().slice(0, 120),
            href: element.href || "",
            src: element.src || "",
            visible: !!(element.offsetWidth || element.offsetHeight || element.getClientRects().length)
          }));
        """.trimIndent()
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
}

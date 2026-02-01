package com.sangita.grantha.backend.api.services.scraping

import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.nodes.Node
import org.jsoup.nodes.TextNode

class HtmlTextExtractor(
    private val maxChars: Int = 120_000,
    private val selectors: List<String> = listOf(
        "div.post-body",
        "div.post",
        "article",
        "div.post-content"
    )
) {
    data class ExtractedContent(
        val text: String,
        val title: String? = null
    )

    fun extract(html: String, baseUrl: String? = null): ExtractedContent {
        val document = if (baseUrl.isNullOrBlank()) {
            Jsoup.parse(html)
        } else {
            Jsoup.parse(html, baseUrl)
        }

        document.select("script, style, nav, footer, header, form, aside, noscript").remove()

        val mainElement = selectors.asSequence()
            .mapNotNull { selector -> document.selectFirst(selector) }
            .firstOrNull { it.text().isNotBlank() }
            ?: document.body()

        val title = document.title().trim().takeIf { it.isNotBlank() }
        val rawText = extractStructuredText(mainElement)

        val cleaned = normalizeText(rawText)
        val trimmed = if (cleaned.length > maxChars) cleaned.substring(0, maxChars) else cleaned

        return ExtractedContent(text = trimmed, title = title)
    }

    private fun extractStructuredText(root: Element): String {
        val builder = StringBuilder()
        val blockTags = setOf(
            "p",
            "li",
            "h1",
            "h2",
            "h3",
            "h4",
            "h5",
            "h6",
            "pre",
            "blockquote"
        )

        fun appendNewline() {
            if (builder.isNotEmpty() && builder.last() != '\n') {
                builder.append('\n')
            }
        }

        fun appendText(text: String) {
            if (text.isBlank()) return
            if (builder.isNotEmpty() && builder.last() != '\n') {
                builder.append(' ')
            }
            builder.append(text)
        }

        fun walk(node: Node) {
            when (node) {
                is TextNode -> appendText(node.text())
                is Element -> {
                    val tag = node.tagName().lowercase()
                    val isBlock = tag in blockTags
                    if (isBlock) appendNewline()

                    if (tag == "br") {
                        appendNewline()
                    } else if (tag == "a") {
                        val linkText = node.text().trim()
                        val href = node.absUrl("href").ifBlank { node.attr("href") }.trim()
                        if (linkText.isNotBlank()) {
                            if (href.isNotBlank()) {
                                appendText("$linkText ($href)")
                            } else {
                                appendText(linkText)
                            }
                        }
                    } else {
                        node.childNodes().forEach { walk(it) }
                    }

                    if (isBlock) appendNewline()
                }
                else -> node.childNodes().forEach { walk(it) }
            }
        }

        walk(root)
        return builder.toString()
    }

    private fun normalizeText(raw: String): String {
        return raw
            .replace(Regex("[\\t\\u000B\\u000C\\r]+"), " ")
            .replace(Regex(" +"), " ")
            .replace(Regex("\\n\\s*\\n+"), "\\n\\n")
            .trim()
    }
}

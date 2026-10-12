package cn.xiaobai.acgfaka.user.core

/** 富文本清洗：把后台商品说明/公告里的 HTML 还原成纯文本，去掉 style/script 整块和标签，解码实体。 */
object HtmlText {
    fun clean(raw: String?): String {
        if (raw.isNullOrBlank()) return ""
        var s = raw
        // 整块删除 style / script，否则里面的 CSS/JS 会当正文显示
        s = s.replace(Regex("<style[^>]*>[\\s\\S]*?</style>", RegexOption.IGNORE_CASE), "")
        s = s.replace(Regex("<script[^>]*>[\\s\\S]*?</script>", RegexOption.IGNORE_CASE), "")
        // 换行类标签转成换行
        s = s.replace(Regex("<br\\s*/?>", RegexOption.IGNORE_CASE), "\n")
        s = s.replace(Regex("</p>", RegexOption.IGNORE_CASE), "\n")
        s = s.replace(Regex("</div>", RegexOption.IGNORE_CASE), "\n")
        s = s.replace(Regex("</li>", RegexOption.IGNORE_CASE), "\n")
        // 去掉所有剩余标签
        s = s.replace(Regex("<[^>]+>"), "")
        // 解码常见 HTML 实体
        s = s.replace("&nbsp;", " ")
        s = s.replace("&amp;", "&")
        s = s.replace("&lt;", "<")
        s = s.replace("&gt;", ">")
        s = s.replace("&quot;", "\"")
        s = s.replace("&#39;", "'")
        s = s.replace("&ldquo;", "“").replace("&rdquo;", "”")
        s = s.replace("&hellip;", "…").replace("&mdash;", "—")
        // 合并多余空行
        s = s.replace(Regex("[ \\t]+"), " ")
        s = s.replace(Regex("\\n{3,}"), "\n\n")
        return s.trim()
    }

    /** 单行清洗，用于公告标题类展示 */
    fun oneLine(raw: String?): String = clean(raw).replace(Regex("\\s+"), " ").trim()
}

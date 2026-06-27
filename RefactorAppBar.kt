import java.io.File
import java.util.regex.Pattern

fun main() {
    val dir = File("d:/projects/gym/composeApp/src/commonMain/kotlin/com/ananta/gym/features")
    val files = dir.walk().filter { it.extension == "kt" }.toList()
    
    var replacedFiles = 0
    for (file in files) {
        val content = file.readText()
        if (!content.contains("CenterAlignedTopAppBar")) continue

        // Basic parsing isn't perfect, but we can extract common structures
        // Most of them look like:
        // CenterAlignedTopAppBar(
        //   title = { Text(text = "...", ...) },
        //   navigationIcon = { IconButton(onClick = { ... }) { ... } },
        //   colors = ...
        // )
        
        // This regex looks for title Text format and navigationIcon onClick format.
        // It's a heuristic but should capture the main pieces.
        var newContent = content
        val titleMatcher = Pattern.compile("text\\s*=\\s*(\"[^\"]+\"|when\\s*\\([^)]+\\)\\s*\\{[^}]+\\})", Pattern.DOTALL).matcher(content)
        val onClickMatcher = Pattern.compile("IconButton\\s*\\(\\s*onClick\\s*=\\s*([^)]+)\\s*\\)").matcher(content)
        
        // Let's use a simpler token-based loop to replace the block
        val startStr = "CenterAlignedTopAppBar("
        while (newContent.contains(startStr)) {
            val startIdx = newContent.indexOf(startStr)
            var endIdx = startIdx + startStr.length
            var bracketCount = 1
            while (endIdx < newContent.length && bracketCount > 0) {
                val c = newContent[endIdx]
                if (c == '(') bracketCount++
                if (c == ')') bracketCount--
                endIdx++
            }
            
            val block = newContent.substring(startIdx, endIdx)
            
            // Extract title
            val titleMatch = Regex("Text\\s*\\(\\s*(?:text\\s*=\\s*)?(\"[^\"]+\"|when\\s*\\([^)]+\\)\\s*\\{[^}]+\\}|if\\s*\\([^)]+\\)\\s*\"[^\"]+\"\\s*else\\s*\"[^\"]+\")").find(block)
            val titleText = titleMatch?.groupValues?.get(1) ?: "\"Unknown\""

            // Extract onClick
            val onClickMatch = Regex("IconButton\\s*\\(\\s*onClick\\s*=\\s*([^)]+)\\s*\\)").find(block)
            var onClickText = onClickMatch?.groupValues?.get(1)
            
            // If the onClick block has braces, adjust bounds
            if (onClickText != null && onClickText.startsWith("{")) {
                // simple heuristic: find matching brace or just take everything if it's `{ navigator?.pop() }`
                val m = Regex("\\{\\s*navigator[^}]+\\}").find(block)
                if (m != null) {
                    onClickText = m.value
                } else if (onClickText.contains("onBackClick")) {
                    onClickText = "onBackClick"
                } else if (onClickText.contains("onNavigateBack")) {
                    onClickText = "onNavigateBack"
                }
            } else if (onClickText == null) {
                // maybe it's missing or no navigation icon
                 if (block.contains("onBackClick")) {
                     onClickText = "onBackClick"
                 }
            }
            
            // Extract actions
            val actionsMatch = Regex("actions\\s*=\\s*(\\{[^}]+\\})", RegexOption.DOT_MATCHES_ALL).find(block)
            val actionsText = actionsMatch?.groupValues?.get(1)

            val replacer = StringBuilder()
            replacer.append("gym.trak.studio.components.GymAppBar(\n")
            replacer.append("                    title = $titleText,\n")
            if (onClickText != null) {
                replacer.append("                    onBackClick = $onClickText")
            }
            if (actionsText != null) {
                if (onClickText != null) replacer.append(",\n")
                replacer.append("                    actions = $actionsText")
            }
            replacer.append("\n                )")
            
            newContent = newContent.replaceRange(startIdx, endIdx, replacer.toString())
        }
        
        if (newContent != content) {
            file.writeText(newContent)
            replacedFiles++
            println("Updated: \${file.name}")
        }
    }
    println("Total files updated: \$replacedFiles")
}

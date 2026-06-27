package in.gym.trak.studio.

import org.junit.Test
import java.io.File
import kotlin.test.assertTrue

class PatchAppBarsTest {

    @Test
    fun testPatchAppBars() {
        var count = 0
        val featuresDir = File("composeApp/src/commonMain/kotlin/com/ananta/gym/features")
        
        fun parseAndReplace(content: String): String {
            var newContent = content
            val startPattern = "CenterAlignedTopAppBar("
            while (newContent.contains(startPattern)) {
                val startIdx = newContent.indexOf(startPattern)
                var bracketCount = 0
                var pos = startIdx + startPattern.length
                while (pos < newContent.length) {
                    when (newContent[pos]) {
                        '(' -> bracketCount++
                        ')' -> {
                            bracketCount--
                            if (bracketCount < 0) break
                        }
                    }
                    pos++
                }
                if (bracketCount >= 0) break // Failed to balance

                val endIdx = pos
                val originalBlock = newContent.substring(startIdx, endIdx + 1)
                
                // Extract Title
                val titleRegex = Regex("title\\s*=\\s*\\{\\s*Text\\s*\\(\\s*(?:text\\s*=\\s*)?(\"[^\"]+\"|when\\s*\\([A-Za-z0-9_]+\\)\\s*\\{[^}]+\\}).*?\\)\\s*\\}", RegexOption.DOT_MATCHES_ALL)
                val titleMatch = titleRegex.find(originalBlock)
                val titleText = titleMatch?.groupValues?.get(1) ?: "\"\""
                
                // Extract onClick
                val onClickRegex = Regex("IconButton\\s*\\(\\s*onClick\\s*=\\s*([^)]+)\\)", RegexOption.DOT_MATCHES_ALL)
                var onClickText = onClickRegex.find(originalBlock)?.groupValues?.get(1)
                
                // if onClick is something like `{ navigator?.pop() }` we capture that precisely
                if (onClickText == null) {
                    onClickText = "{ navigator?.pop() }"
                }
                
                // Extract actions if present
                val actionsRegex = Regex("actions\\s*=\\s*(\\{[^}]+\\})", RegexOption.DOT_MATCHES_ALL)
                val actionsText = actionsRegex.find(originalBlock)?.groupValues?.get(1)
                
                val builder = StringBuilder()
                builder.append("gym.trak.studio.components.GymAppBar(\n")
                
                // Make sure to pad properly
                builder.append("                    title = $titleText,\n")
                if (onClickText.isNotEmpty()) {
                    builder.append("                    onBackClick = $onClickText")
                }
                if (actionsText != null) {
                    builder.append(",\n                    actions = $actionsText\n")
                } else {
                    builder.append("\n")
                }
                builder.append("                )")
                
                newContent = newContent.replaceRange(startIdx, endIdx + 1, builder.toString())
            }
            return newContent
        }

        featuresDir.walkTopDown().filter { it.extension == "kt" }.forEach { file ->
            val original = file.readText()
            val replaced = parseAndReplace(original)
            if (original != replaced) {
                file.writeText(replaced)
                count++
                println("Patched: \${file.name}")
            }
        }
        
        println("Patched \$count files")
        assertTrue(true)
    }
}

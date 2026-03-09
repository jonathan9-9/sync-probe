package com.jonathan.syncprobe.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MarkdownUtils {
    private static final Pattern CODE_BLOCK_PATTERN = Pattern.compile("(?s)```[\\w-]*\\R(.*?)```");
    private static final Pattern IMAGE_PATTERN = Pattern.compile("!\\[[^]]*]\\([^)]*\\)");
    private static final Pattern LINK_PATTERN = Pattern.compile("\\[([^]]+)]\\([^)]*\\)");
    private static final Pattern INLINE_CODE_PATTERN = Pattern.compile("`([^`]+)`");
    private static final Pattern HEADING_PATTERN = Pattern.compile("(?m)^\\s{0,3}#{1,6}\\s*");
    private static final Pattern BLOCKQUOTE_PATTERN = Pattern.compile("(?m)^\\s{0,3}>\\s?");
    private static final Pattern LIST_PATTERN = Pattern.compile("(?m)^\\s*([-*+]\\s+|\\d+\\.\\s+)");
    private static final Pattern HTML_PATTERN = Pattern.compile("(?s)<[^>]+>");
    private static final Pattern EMPHASIS_PATTERN = Pattern.compile("(\\*\\*|__|\\*|_|~~)");
    private static final Pattern MULTISPACE_PATTERN = Pattern.compile("[ \\t\\x0B\\f\\r]+");
    private static final Pattern MULTINEWLINE_PATTERN = Pattern.compile("\\n{3,}");

    public static String stripMarkdown(String markdown){
        if (markdown == null || markdown.isBlank()) {
            return "";
        }

        String text = markdown;
        text = CODE_BLOCK_PATTERN.matcher(text).replaceAll(" $1 ");
        text = IMAGE_PATTERN.matcher(text).replaceAll(" ");
        text = LINK_PATTERN.matcher(text).replaceAll("$1");
        text = INLINE_CODE_PATTERN.matcher(text).replaceAll("$1");
        text = HEADING_PATTERN.matcher(text).replaceAll("");
        text = BLOCKQUOTE_PATTERN.matcher(text).replaceAll("");
        text = LIST_PATTERN.matcher(text).replaceAll("");
        text = HTML_PATTERN.matcher(text).replaceAll(" ");
        text = EMPHASIS_PATTERN.matcher(text).replaceAll("");
        text = MULTISPACE_PATTERN.matcher(text).replaceAll(" ");
        text = MULTINEWLINE_PATTERN.matcher(text).replaceAll("\n\n");
        return text.trim();
    }

    public static List<String> extractCodeBlocks(String markdown){
        List<String> blocks = new ArrayList<>();
        if (markdown == null || markdown.isBlank()) {
            return blocks;
        }

        Matcher matcher = CODE_BLOCK_PATTERN.matcher(markdown);
        while (matcher.find()) {
            String block = matcher.group(1);
            if (block != null && !block.isBlank()) {
                blocks.add(block.trim());
            }
        }
        return blocks;
    }
}

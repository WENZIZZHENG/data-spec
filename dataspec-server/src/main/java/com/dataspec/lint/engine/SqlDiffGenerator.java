package com.dataspec.lint.engine;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 生成原 SQL 与修正 SQL 的行级 unified diff。
 */
public class SqlDiffGenerator {

    public String generate(String originalSql, String fixedSql) {
        if (originalSql == null || fixedSql == null || Objects.equals(originalSql, fixedSql)) {
            return null;
        }

        List<String> originalLines = originalSql.lines().toList();
        List<String> fixedLines = fixedSql.lines().toList();
        if (originalLines.equals(fixedLines)) {
            return null;
        }

        List<String> diffLines = buildDiffLines(originalLines, fixedLines);
        if (diffLines.stream().noneMatch(line -> line.startsWith("-") || line.startsWith("+"))) {
            return null;
        }

        List<String> output = new ArrayList<>();
        output.add("--- original.sql");
        output.add("+++ fixed.sql");
        output.add("@@");
        output.addAll(diffLines);
        return String.join("\n", output);
    }

    private List<String> buildDiffLines(List<String> originalLines, List<String> fixedLines) {
        int[][] lcs = lcs(originalLines, fixedLines);
        List<String> output = new ArrayList<>();
        int i = 0;
        int j = 0;
        while (i < originalLines.size() && j < fixedLines.size()) {
            if (Objects.equals(originalLines.get(i), fixedLines.get(j))) {
                output.add(" " + originalLines.get(i));
                i++;
                j++;
            } else if (lcs[i + 1][j] >= lcs[i][j + 1]) {
                output.add("-" + originalLines.get(i));
                i++;
            } else {
                output.add("+" + fixedLines.get(j));
                j++;
            }
        }
        while (i < originalLines.size()) {
            output.add("-" + originalLines.get(i));
            i++;
        }
        while (j < fixedLines.size()) {
            output.add("+" + fixedLines.get(j));
            j++;
        }
        return output;
    }

    private int[][] lcs(List<String> left, List<String> right) {
        int[][] dp = new int[left.size() + 1][right.size() + 1];
        for (int i = left.size() - 1; i >= 0; i--) {
            for (int j = right.size() - 1; j >= 0; j--) {
                if (Objects.equals(left.get(i), right.get(j))) {
                    dp[i][j] = dp[i + 1][j + 1] + 1;
                } else {
                    dp[i][j] = Math.max(dp[i + 1][j], dp[i][j + 1]);
                }
            }
        }
        return dp;
    }
}

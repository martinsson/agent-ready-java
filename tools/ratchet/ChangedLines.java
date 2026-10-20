package ratchet;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** The line numbers added or modified since a base commit, per file, as they are on disk. */
final class ChangedLines {

  private static final Pattern NEW_RANGE = Pattern.compile("^@@ -\\S+ \\+(\\d+)(?:,(\\d+))? @@");

  private ChangedLines() {}

  /** Committed and uncommitted changes under {@code dir}, and its untracked files as a whole. */
  static Map<String, SortedSet<Integer>> since(String baseRef, String dir) throws IOException {
    Map<String, SortedSet<Integer>> changed =
        parse(
            GitTree.run(
                "git",
                "diff",
                "-U0",
                "--no-color",
                "--no-ext-diff",
                "--no-prefix",
                baseRef,
                "--",
                dir));
    for (String file :
        GitTree.run("git", "ls-files", "--others", "--exclude-standard", "--", dir)) {
      changed.put(file, range(1, Files.readAllLines(Path.of(file)).size()));
    }
    return changed;
  }

  /** Reads the output of {@code git diff -U0 --no-prefix}. */
  static Map<String, SortedSet<Integer>> parse(List<String> diff) {
    Map<String, SortedSet<Integer>> changed = new TreeMap<>();
    String file = "";
    boolean header = false;
    for (String line : diff) {
      if (line.startsWith("diff --git ")) {
        header = true;
        file = "";
      } else if (header && line.startsWith("+++ ")) {
        file = line.equals("+++ /dev/null") ? "" : line.substring(4);
      } else if (line.startsWith("@@")) {
        header = false;
        SortedSet<Integer> lines = newLines(line);
        if (!file.isEmpty() && !lines.isEmpty()) {
          changed.computeIfAbsent(file, f -> new TreeSet<>()).addAll(lines);
        }
      }
    }
    return changed;
  }

  /** "@@ -a,b +c,d @@" covers lines c to c+d-1 of the new file; d is 1 when omitted. */
  static SortedSet<Integer> newLines(String hunkHeader) {
    Matcher m = NEW_RANGE.matcher(hunkHeader);
    if (!m.find()) {
      return new TreeSet<>();
    }
    int count = m.group(2) == null ? 1 : Integer.parseInt(m.group(2));
    return range(Integer.parseInt(m.group(1)), count);
  }

  private static SortedSet<Integer> range(int first, int count) {
    SortedSet<Integer> lines = new TreeSet<>();
    for (int i = first; i < first + count; i++) {
      lines.add(i);
    }
    return lines;
  }
}

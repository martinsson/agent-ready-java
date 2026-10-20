package ratchet;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Lines changed since the base commit must be covered by tests, whatever the global ratio is. Reads
 * the JaCoCo XML report of the build that just ran, so it needs nothing but the JDK.
 */
final class ChangedLinesCoverage {

  static final String REPORT = "target/site/jacoco/jacoco.xml";
  static final String SOURCES = "src/main/java";
  static final String MINIMUM = "coverage.changed.minimum";
  private static final String GATE_EXECUTION = "jacoco-check";

  /** The executable changed lines of one file, split into covered and missed. */
  record FileLines(String path, SortedSet<Integer> covered, SortedSet<Integer> missed) {}

  private ChangedLinesCoverage() {}

  /**
   * Integration tests only run with --full, so a fast run leaves out what the global coverage gate
   * leaves out (its {@code <exclude>} patterns in the pom).
   */
  static int check(String baseRef, boolean full, PrintStream out) throws IOException {
    Path report = Path.of(REPORT);
    if (!Files.exists(report)) {
      out.println("COVERAGE: no " + REPORT + " after the build, cannot check the changed lines");
      return 1;
    }
    String pom = Files.readString(Path.of(RatchetCheck.POM));
    String minimum = Limits.fromPom(pom).getOrDefault(MINIMUM, "0");
    List<PathMatcher> excluded = full ? List.of() : gateExcludes(pom);
    List<FileLines> files =
        measure(
            Xml.parse(Files.readString(report)), ChangedLines.since(baseRef, SOURCES), excluded);
    List<String> problems = problems(files, Double.parseDouble(minimum), baseRef);
    problems.forEach(out::println);
    return problems.isEmpty() ? 0 : 1;
  }

  static List<FileLines> measure(
      Document report, Map<String, SortedSet<Integer>> changed, List<PathMatcher> excluded) {
    List<FileLines> files = new ArrayList<>();
    for (Element pkg : Xml.elements(report, "package")) {
      String dir = pkg.getAttribute("name");
      for (Element source : Xml.children(pkg, "sourcefile")) {
        String name = source.getAttribute("name");
        String classFile = (dir.isEmpty() ? "" : dir + "/") + name.replace(".java", ".class");
        String path = SOURCES + "/" + (dir.isEmpty() ? "" : dir + "/") + name;
        SortedSet<Integer> lines = changed.get(path);
        if (lines != null && excluded.stream().noneMatch(m -> m.matches(Path.of(classFile)))) {
          files.add(split(path, source, lines));
        }
      }
    }
    return files;
  }

  static List<String> problems(List<FileLines> files, double minimum, String baseRef) {
    int covered = files.stream().mapToInt(f -> f.covered().size()).sum();
    int missed = files.stream().mapToInt(f -> f.missed().size()).sum();
    int total = covered + missed;
    if (total == 0 || covered >= minimum * total) {
      return List.of();
    }
    List<String> problems = new ArrayList<>();
    problems.add(
        "COVERAGE: %d of %d changed lines since %s are not covered by tests (%d%%, minimum %d%%):"
            .formatted(missed, total, baseRef, covered * 100 / total, Math.round(minimum * 100)));
    files.stream()
        .filter(f -> !f.missed().isEmpty())
        .forEach(f -> problems.add("  " + f.path() + ": " + ranges(f.missed())));
    problems.add(
        "HINT: write tests that run and assert these lines; target/site/jacoco/index.html shows"
            + " them");
    return problems;
  }

  /** "3-5, 9" for 3, 4, 5, 9. */
  static String ranges(SortedSet<Integer> lines) {
    List<int[]> runs = new ArrayList<>();
    for (int line : lines) {
      if (!runs.isEmpty() && runs.getLast()[1] == line - 1) {
        runs.getLast()[1] = line;
      } else {
        runs.add(new int[] {line, line});
      }
    }
    return String.join(
        ", ", runs.stream().map(r -> r[0] == r[1] ? "" + r[0] : r[0] + "-" + r[1]).toList());
  }

  private static FileLines split(String path, Element source, SortedSet<Integer> changed) {
    SortedSet<Integer> covered = new TreeSet<>();
    SortedSet<Integer> missed = new TreeSet<>();
    for (Element line : Xml.children(source, "line")) {
      int nr = Integer.parseInt(line.getAttribute("nr"));
      if (changed.contains(nr)) {
        (Integer.parseInt(line.getAttribute("ci")) > 0 ? covered : missed).add(nr);
      }
    }
    return new FileLines(path, covered, missed);
  }

  private static List<PathMatcher> gateExcludes(String pom) throws IOException {
    List<PathMatcher> matchers = new ArrayList<>();
    for (Element execution : Xml.elements(Xml.parse(pom), "execution")) {
      List<Element> id = Xml.children(execution, "id");
      if (!id.isEmpty() && id.get(0).getTextContent().trim().equals(GATE_EXECUTION)) {
        for (Element exclude : Xml.elements(execution, "exclude")) {
          String glob = exclude.getTextContent().trim();
          matchers.add(FileSystems.getDefault().getPathMatcher("glob:" + glob));
        }
      }
    }
    return matchers;
  }
}

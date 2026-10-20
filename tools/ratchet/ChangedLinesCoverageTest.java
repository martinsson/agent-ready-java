package ratchet;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;

class ChangedLinesCoverageTest {

  private static final String REPORT =
      """
      <report name="demo">
        <package name="com/acme/domain">
          <sourcefile name="Order.java">
            <line nr="10" mi="0" ci="3" mb="0" cb="0"/>
            <line nr="11" mi="2" ci="0" mb="0" cb="0"/>
            <line nr="12" mi="2" ci="0" mb="0" cb="0"/>
            <line nr="20" mi="0" ci="1" mb="0" cb="0"/>
          </sourcefile>
        </package>
        <package name="com/acme/adapter">
          <sourcefile name="Db.java">
            <line nr="5" mi="4" ci="0" mb="0" cb="0"/>
          </sourcefile>
        </package>
      </report>
      """;

  private static final String ORDER = "src/main/java/com/acme/domain/Order.java";
  private static final String DB = "src/main/java/com/acme/adapter/Db.java";

  @Test
  void onlyChangedExecutableLinesCount() throws IOException {
    List<ChangedLinesCoverage.FileLines> files =
        ChangedLinesCoverage.measure(report(), Map.of(ORDER, lines(9, 10, 11)), List.of());

    assertThat(files)
        .containsExactly(new ChangedLinesCoverage.FileLines(ORDER, lines(10), lines(11)));
  }

  @Test
  void aFastRunLeavesOutWhatTheGlobalGateExcludes() throws IOException {
    List<ChangedLinesCoverage.FileLines> files =
        ChangedLinesCoverage.measure(
            report(),
            Map.of(ORDER, lines(10), DB, lines(5)),
            List.of(FileSystems.getDefault().getPathMatcher("glob:com/acme/adapter/**")));

    assertThat(files).extracting(ChangedLinesCoverage.FileLines::path).containsExactly(ORDER);
  }

  @Test
  void belowTheMinimumListsTheMissedLines() throws IOException {
    List<ChangedLinesCoverage.FileLines> files =
        ChangedLinesCoverage.measure(report(), Map.of(ORDER, lines(10, 11, 12, 20)), List.of());

    assertThat(ChangedLinesCoverage.problems(files, 0.95, "abc123"))
        .startsWith(
            "COVERAGE: 2 of 4 changed lines since abc123 are not covered by tests (50%, minimum"
                + " 95%):",
            "  " + ORDER + ": 11-12");
  }

  @Test
  void atTheMinimumOrWithoutExecutableLinesPasses() throws IOException {
    List<ChangedLinesCoverage.FileLines> covered =
        ChangedLinesCoverage.measure(report(), Map.of(ORDER, lines(10, 20)), List.of());
    List<ChangedLinesCoverage.FileLines> none =
        ChangedLinesCoverage.measure(report(), Map.of(ORDER, lines(1, 2)), List.of());

    assertThat(ChangedLinesCoverage.problems(covered, 0.95, "abc123")).isEmpty();
    assertThat(ChangedLinesCoverage.problems(none, 0.95, "abc123")).isEmpty();
  }

  @Test
  void consecutiveLinesReadAsRanges() {
    assertThat(ChangedLinesCoverage.ranges(lines(3, 4, 5, 9, 11, 12))).isEqualTo("3-5, 9, 11-12");
  }

  private static Document report() throws IOException {
    return Xml.parse(REPORT);
  }

  private static SortedSet<Integer> lines(Integer... numbers) {
    return new TreeSet<>(List.of(numbers));
  }
}

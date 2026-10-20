package ratchet;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class ChangedLinesTest {

  @Test
  void hunksGiveTheNewLineNumbers() {
    List<String> diff =
        List.of(
            "diff --git src/A.java src/A.java",
            "--- src/A.java",
            "+++ src/A.java",
            "@@ -3,0 +4,2 @@ class A {",
            "+  int a;",
            "+  int b;",
            "@@ -10 +12 @@",
            "-  old",
            "+  new");

    assertThat(ChangedLines.parse(diff)).containsOnlyKeys("src/A.java");
    assertThat(ChangedLines.parse(diff).get("src/A.java")).containsExactly(4, 5, 12);
  }

  @Test
  void deletionsChangeNoLine() {
    List<String> diff =
        List.of(
            "diff --git src/A.java src/A.java",
            "--- src/A.java",
            "+++ src/A.java",
            "@@ -7,2 +6,0 @@",
            "diff --git src/B.java src/B.java",
            "--- src/B.java",
            "+++ /dev/null",
            "@@ -1,3 +0,0 @@");

    assertThat(ChangedLines.parse(diff)).isEmpty();
  }

  @Test
  void anAddedLineThatLooksLikeAHeaderIsContent() {
    List<String> diff =
        List.of(
            "diff --git src/A.java src/A.java",
            "--- src/A.java",
            "+++ src/A.java",
            "@@ -1,0 +2 @@",
            "+++ not a header");

    assertThat(ChangedLines.parse(diff).get("src/A.java")).containsExactly(2);
  }
}

# Reports

Details on where Project Doctor writes its reports and when. For the task overview, see
[Tasks & CLI Options](../README.md#tasks--cli-options).

## Output locations

| Task                     | Scope          | Output                                                   |
| ------------------------ | -------------- | -------------------------------------------------------- |
| `doctorPopulate`         | per subproject | `<root>/build/doctor/<module-dir>/project_composition.json` |
| `doctorCheck`            | per subproject | `<root>/build/doctor/<module-dir>/result.json`, `checkstyle.xml` |
| `doctorCheck`            | root           | `build/doctor/merged.json`                               |
| `doctorGenerateBaseline` | per subproject | `<root>/build/doctor/<module-dir>/baseline_result.json`  |
| `doctorGenerateBaseline` | root           | configured `baselineFile`                                |

`<module-dir>` is the module directory relative to the root, not necessarily its Gradle path.
Module reports use `<root>/build/doctor` even if you relocate a module's build directory.
The root merged report uses the root's configured build directory.

## When reports are generated

- Without `--summary`, a module reports and fails on non-baselined errors. This can prevent
  the root aggregation task from running.
- Module Checkstyle XML is generated only for non-baselined violations in non-summary mode.
  There is no root Checkstyle report.
- Summary mode writes raw module JSON when violations exist, skipping baseline filtering,
  console reporting, and XML generation at the module level. The root still filters the baseline.
- Reports are not a guaranteed empty-results signal. Module JSON/XML are removed when no raw
  violations exist, but existing XML can remain unchanged in summary mode or when all violations
  are baselined. Root `merged.json` is written only for non-empty filtered results and can remain
  from a previous run. Use the current console report and task outcome to judge a clean check.

## The `--summary` option

- `doctorCheck --summary` (the task selector) passes the option to module checks as well as the
  root task.
- `:doctorCheck --summary` only gives the option to the root, where it is ignored.
- A module-only invocation such as `:feature:home:doctorCheck --summary` does not run aggregation.
- Summary mode does not suppress dependency-resolution or other execution failures.

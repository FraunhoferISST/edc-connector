# PMD Rule Filtering & Violation Resolution Guide

This guide explains how to:

* Add PMD rule filters by category (e.g. performance, security)
* Run and customize the PMD filtering Python script
* Fix PMD violations in Java
* Re-run PMD and verify resolution using automated scripts

---

## 📁 Directory Structure

```
/resources/edc-pmd-ruleset.xml    # Custom PMD rule configuration
/resources/pmd_rules/performance.txt        # Rules to track under "performance"
/resources/pmd_rules/security.txt           # (Optional) Security rules
/scripts/filter_pmd_performance.py  # Python script to filter violations
/build/reports/pmd/               # PMD XML report output from Gradle
```

---

## 🔍 Step 1: Adding Rules to `performance.txt` or `security.txt`

To track specific types of PMD rules:

1. Navigate to:

   ```
   /resources/pmd_rules/
   ```
2. Open or create a file:

    * `performance.txt` → for performance rules
    * `security.txt` → for security rules
3. Add rule names, one per line (exactly as PMD identifies them):
   Example (`performance.txt`):

   ```txt
    AvoidFileStream
    AvoidInstantiatingObjectsInLoops
    RedundantFieldInitializer
    UseIndexOfChar
    AppendCharacterWithChar
    ...
    ````

✅ You can get rule names from PMD documentation or by reading PMD XML output.

---

## 🐍 Step 2: How the Python Script Works

The Python script:
- Recursively scans PMD XML reports under your project
- Filters violations by rule name (using `performance.txt`, etc.)
- Outputs results into:
- `scripts/filtered-violations/performance_violations.md`

### 🔧 To Run:
```bash
# From project root (where the script and rule files exist)
python scripts/filter_pmd_performance.py
````

You should see:

```
📄 Loaded 25 rules from pmd_rules/performance.txt
🔍 Scanning PMD reports...
✅ Found 18 violations
📁 Saved performance_violations..md
```

---

## ❗ Step 3: Example – Fixing a Violation

### 👇 Violation Entry Example:

```
| 1 | TestUtils.java | 29 | RedundantFieldInitializer | Avoid using redundant field initializer for 'buildRoot' |
```

### 💡 Explanation:

Java auto-initializes `File` fields to `null`, so this:

```java
private static File buildRoot = null;
```

Should be changed to:

```java
private static File buildRoot;
```

---

## 🔄 Step 4: Rerun PMD on Only One Module/Package

To rerun PMD only for the affected module:

```bash
# Run PMD for just this module
./gradlew :extensions:control-plane:callback:callback-event-dispatcher:pmdMain
```

📌 This will regenerate the report in: `build/reports/pmd/main.xml`

---

## 🔄 Step 5: Rerun the Python Filter Script

After fixing and rerunning PMD:

```bash
python scripts/filter_pmd_performance.py
```

Check `performance_violations.md` and verify that the fixed entry is gone.

✅ **If it's gone — the issue is resolved.**

---

## 🧠 Summary Workflow

| Step | Action                                                |
| ---- | ----------------------------------------------------- |
| 📝   | Add rules to `performance.txt`, `security.txt`, etc.  |
| 🐍   | Run `filter_pmd_performance.py` to get violation list |
| 💪   | Fix Java code based on rule message                   |
| 🔄   | Re-run PMD via `./gradlew pmdMain` (or `pmdAll`)      |
| 🔄   | Rerun script to confirm fix                           |
| ✅    | Violation disappears = success                        |

---

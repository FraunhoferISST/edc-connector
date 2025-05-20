import os
import xml.etree.ElementTree as ET

def load_rules_from_file(filepath):
    with open(filepath, "r") as f:
        return set(line.strip() for line in f if line.strip())

def parse_pmd_report(file_path, rules_to_filter):
    violations = []
    try:
        tree = ET.parse(file_path)
        root = tree.getroot()
        for file_elem in root.findall("{http://pmd.sourceforge.net/report/2.0.0}file"):
            file_name = file_elem.attrib.get("name")
            for violation in file_elem.findall("{http://pmd.sourceforge.net/report/2.0.0}violation"):
                rule = violation.attrib.get("rule")
                if rule in rules_to_filter:
                    line = violation.attrib.get("beginline")
                    message = violation.text.strip() if violation.text else ""
                    violations.append((file_name, line, rule, message))
    except ET.ParseError:
        print(f"❌ Parse error in {file_path}")
    return violations

def scan_pmd_reports(base_dir, rules_to_filter):
    all_violations = []
    for dirpath, _, files in os.walk(base_dir):
        for file in files:
            if file.endswith(".xml"):
                filepath = os.path.join(dirpath, file)
                try:
                    violations = parse_pmd_report(filepath, rules_to_filter)
                    if violations:
                        all_violations.extend(violations)
                except Exception as e:
                    print(f"❌ Error reading {filepath}: {e}")
    return all_violations

def save_reports(violations):
    # Dynamically find the directory this script is in
    script_dir = os.path.dirname(os.path.abspath(__file__))

    # Create the target path inside scripts/filtered-violations/
    output_dir = os.path.join(script_dir, "filtered-violations")
    os.makedirs(output_dir, exist_ok=True)

    # Final file path
    output_file = os.path.join(output_dir, "performance_violations.md")

    # Write the file
    with open(output_file, "w") as mdfile:
        mdfile.write("# PMD Performance Violations\n\n")
        mdfile.write("| # | File | Line | Rule | Message |\n")
        mdfile.write("|----|------|------|------|---------|\n")
        for idx, (file, line, rule, msg) in enumerate(violations, start=1):
            mdfile.write(f"| {idx} | {file} | {line} | {rule} | {msg} |\n")

    print(f"📁 Saved {output_file}")


if __name__ == "__main__":
    # Adjust these paths if needed
    rules_file = "resources/pmd_rules/performance.txt"
    reports_dir = ".."

    rules = load_rules_from_file(rules_file)
    print(f"📄 Loaded {len(rules)} rules from {rules_file}")

    print("🔍 Scanning PMD reports...")
    violations = scan_pmd_reports(reports_dir, rules)
    print(f"✅ Found {len(violations)} violations")

    save_reports(violations)
    print("📁 Saved performance_violations.md")

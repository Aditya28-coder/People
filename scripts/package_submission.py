"""Create a clean source ZIP using only Python's standard library."""
from pathlib import Path
from zipfile import ZIP_DEFLATED, ZipFile

root = Path(__file__).resolve().parent.parent
output = root / "output" / "employee-management-source.zip"
output.parent.mkdir(exist_ok=True)
excluded = {".git", ".idea", ".vscode", "target", "data", "tmp", "output", "__pycache__", "node_modules"}
with ZipFile(output, "w", ZIP_DEFLATED) as archive:
    for path in sorted(root.rglob("*")):
        relative = path.relative_to(root)
        if not path.is_file() or any(part in excluded for part in relative.parts):
            continue
        if path.name == ".env" or path.suffix == ".log":
            continue
        archive.write(path, Path("employee-management") / relative)
print(output)

"""Reject tracked environment files; templates remain subject to Gitleaks."""
from pathlib import PurePosixPath
import subprocess
import sys


def forbidden(path: str) -> bool:
    name = PurePosixPath(path).name.lower()
    if name in {".env.example", ".env.sample", ".env.template"}:
        return False
    return name == ".env" or name.startswith(".env.") or name.endswith(".env") or ".env." in name


if __name__ == "__main__":
    files = subprocess.check_output(["git", "ls-files", "-z"]).decode("utf-8").split("\0")
    rejected = [path for path in files if path and forbidden(path)]
    if rejected:
        # Do not print file contents, credentials, or untrusted filenames.
        sys.exit(f"Encontrados {len(rejected)} arquivos de ambiente versionados. Remova-os do índice Git.")
    print("Nenhum arquivo de ambiente proibido está versionado.")

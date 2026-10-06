"""Exercise the workflow's actual Bash scripts without credentials or publishing."""

import os
from pathlib import Path
import re
import subprocess
import tempfile
import textwrap
import unittest


WORKFLOW = Path(__file__).resolve().parents[1] / "workflows" / "publish.yml"
ROOT = WORKFLOW.parents[2]


def step_script(name):
    # Read this workflow's indented run block without a third-party YAML dependency.
    step = WORKFLOW.read_text().split(f"      - name: {name}\n", 1)[1]
    step = step.split("\n      - name:", 1)[0]
    block = re.search(r"        run: \|\n((?:          .*\n?)+)", step)
    if block is None:
        raise AssertionError(f"No Bash run block for {name}")
    return textwrap.dedent(block.group(1))


class PublishWorkflowTest(unittest.TestCase):
    def run_step(self, name, **env):
        # Match GitHub's fail-fast Bash behavior. No real credentials are supplied.
        return subprocess.run(
            ["bash", "--noprofile", "--norc", "-eo", "pipefail", "-c", step_script(name)],
            env={**os.environ, **env},
            capture_output=True,
            text=True,
        )

    def test_version_resolution(self):
        cases = [
            ("", "refs/heads/main", "", ""),
            ("0.2.0-SNAPSHOT", "refs/heads/main", "", "0.2.0-SNAPSHOT"),
            ("", "refs/tags/v0.2.0", "v0.2.0", "0.2.0"),
            ("", "refs/tags/0.2.0", "0.2.0", "0.2.0"),
            ("0.3.0", "refs/tags/v0.2.0", "v0.2.0", "0.3.0"),
        ]
        for manual, ref, tag, expected in cases:
            with self.subTest(manual=manual, ref=ref), tempfile.TemporaryDirectory() as tmp:
                output = Path(tmp) / "output"
                result = self.run_step(
                    "Determine version", INPUT_VERSION=manual, GITHUB_REF=ref,
                    GITHUB_REF_NAME=ref.rsplit("/", 1)[1], RELEASE_TAG=tag,
                    GITHUB_OUTPUT=str(output),
                )
                self.assertEqual(result.returncode, 0, result.stderr)
                self.assertEqual(output.read_text(), f"version={expected}\n")

    def test_signing_requirement(self):
        for version in ("", "0.2.0-SNAPSHOT", "0.2.0"):
            for key in ("", "dummy-key"):
                with self.subTest(version=version, key=bool(key)):
                    result = self.run_step(
                        "Verify credentials", VERSION=version, SIGNING_KEY=key,
                        MAVEN_CENTRAL_USERNAME="dummy", MAVEN_CENTRAL_PASSWORD="dummy",
                    )
                    expected = 1 if version == "0.2.0" and not key else 0
                    self.assertEqual(result.returncode, expected, result.stdout + result.stderr)

    def test_central_credentials_are_always_required(self):
        for version in ("", "0.2.0-SNAPSHOT", "0.2.0"):
            for username, password in (("", "dummy"), ("dummy", ""), ("", "")):
                with self.subTest(version=version, username=username, password=password):
                    result = self.run_step(
                        "Verify credentials", VERSION=version, SIGNING_KEY="dummy-key",
                        MAVEN_CENTRAL_USERNAME=username, MAVEN_CENTRAL_PASSWORD=password,
                    )
                    self.assertEqual(result.returncode, 1)
                    self.assertIn("MAVEN_CENTRAL_USERNAME and MAVEN_CENTRAL_PASSWORD", result.stdout)

    def test_snapshots_ignore_missing_or_broken_signing_credentials(self):
        for version in ("", "0.2.0-SNAPSHOT"):
            for key in ("", "not-a-key"):
                with self.subTest(version=version, key=key), tempfile.TemporaryDirectory() as tmp:
                    # If GPG is called at all, this test fails, even on a host without GPG.
                    gpg = Path(tmp) / "gpg"
                    gpg.write_text("#!/bin/sh\nexit 99\n")
                    gpg.chmod(0o755)
                    env_file = Path(tmp) / "env"
                    result = self.run_step(
                        "Verify signing key", VERSION=version, SIGNING_KEY=key,
                        SIGNING_PASSWORD="invalid", GITHUB_ENV=str(env_file),
                        PATH=f"{tmp}{os.pathsep}{os.environ['PATH']}",
                    )
                    self.assertEqual(result.returncode, 0, result.stdout + result.stderr)
                    self.assertFalse(env_file.exists(), "Snapshots must not export signing credentials")

    def test_release_key_cannot_be_missing(self):
        result = self.run_step(
            "Verify signing key", VERSION="0.2.0", SIGNING_KEY="", SIGNING_PASSWORD="",
        )
        self.assertEqual(result.returncode, 1)
        self.assertIn("SIGNING_IN_MEMORY_KEY must be set", result.stdout)

    def test_release_verification_rejects_import_or_signing_failure(self):
        for fail_import in (True, False):
            with self.subTest(fail_import=fail_import), tempfile.TemporaryDirectory() as tmp:
                gpg = Path(tmp) / "gpg"
                gpg.write_text(
                    "#!/bin/sh\nexit 1\n" if fail_import else
                    '#!/bin/sh\ncase "$*" in *--import*) exit 0 ;; *) exit 1 ;; esac\n'
                )
                gpg.chmod(0o755)
                env_file = Path(tmp) / "env"
                result = self.run_step(
                    "Verify signing key", VERSION="0.2.0", SIGNING_KEY="dummy-key",
                    SIGNING_PASSWORD="invalid", GITHUB_ENV=str(env_file), TMPDIR=tmp,
                    PATH=f"{tmp}{os.pathsep}{os.environ['PATH']}",
                )
                self.assertNotEqual(result.returncode, 0)
                self.assertFalse(env_file.exists(), "Failed verification must not export credentials")

    def test_version_is_not_evaluated_as_shell_code(self):
        with tempfile.TemporaryDirectory() as tmp:
            marker = Path(tmp) / "injected"
            version = f"$(touch {marker})"
            output = Path(tmp) / "output"
            result = self.run_step(
                "Determine version", INPUT_VERSION=version, GITHUB_REF="refs/heads/main",
                GITHUB_REF_NAME="main", RELEASE_TAG="", GITHUB_OUTPUT=str(output),
            )
            self.assertEqual(result.returncode, 0, result.stderr)
            self.assertEqual(output.read_text(), f"version={version}\n")
            self.assertFalse(marker.exists())


class PublicationSigningTest(unittest.TestCase):
    def test_snapshots_do_not_configure_signing_with_a_broken_key(self):
        for version in (None, "0.2.0-SNAPSHOT"):
            with self.subTest(version=version):
                args = [
                    "bash", str(ROOT / "gradlew"), "tasks", "--all", "--console=plain",
                    "--no-configuration-cache", "-PsigningInMemoryKey=not-a-key",
                    "-PsigningInMemoryKeyPassword=invalid",
                ]
                if version is not None:
                    args.append(f"-PprojectDoctorVersion={version}")
                result = subprocess.run(
                    args, cwd=ROOT, capture_output=True, text=True, timeout=120,
                )
                self.assertEqual(result.returncode, 0, result.stdout + result.stderr)
                self.assertNotIn("signPluginMavenPublication -", result.stdout)
                self.assertNotIn("checkSigningConfiguration -", result.stdout)


if __name__ == "__main__":
    unittest.main()

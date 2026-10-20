import unittest

from scripts.ci.check_env import forbidden


class EnvironmentFileTests(unittest.TestCase):
    def test_environment_files_in_any_directory(self):
        for path in [".env", ".env.local", "api/.env.production", "config/prod.env", "api/.ENV", "prod.env.backup"]:
            with self.subTest(path=path):
                self.assertTrue(forbidden(path))

    def test_templates_and_regular_files(self):
        for path in [".env.example", "api/.env.template", ".env.sample", "README.md", "environment.py"]:
            with self.subTest(path=path):
                self.assertFalse(forbidden(path))

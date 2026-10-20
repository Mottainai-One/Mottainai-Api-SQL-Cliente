import unittest

from scripts.ci.check_pr import checked


class ChecklistTests(unittest.TestCase):
    def test_checked_item(self):
        self.assertTrue(checked("- [x] O projeto compila sem erros.", "O projeto compila sem erros."))
        self.assertTrue(checked("* [X] O projeto compila sem erros.\r\n", "O projeto compila sem erros."))

    def test_unchecked_or_missing(self):
        for body in ("", "- [ ] O projeto compila sem erros.", "- [x] O projeto compila sem erros. talvez"):
            self.assertFalse(checked(body, "O projeto compila sem erros."))

    def test_hidden_or_example_is_not_evidence(self):
        for body in (
            "<!--\n- [x] O projeto compila sem erros.\n-->",
            "```md\n- [x] O projeto compila sem erros.\n```",
            "~~~\n- [x] O projeto compila sem erros.\n~~~",
            "> - [x] O projeto compila sem erros.",
            "    - [x] O projeto compila sem erros.",
        ):
            self.assertFalse(checked(body, "O projeto compila sem erros."))

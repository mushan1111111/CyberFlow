import unittest

from consumers.account_merge import (
    account_group,
    build_account_merge_lookup,
    canonical_account,
    canonical_data_owners,
)


class AccountMergeTest(unittest.TestCase):
    def test_aliases_are_normalized_to_the_primary_account(self):
        lookup = build_account_merge_lookup({"B-姓名": ["B-账号1", "B-账号2"]})

        self.assertEqual("B-姓名", canonical_account("B-账号1", lookup))
        self.assertEqual("B-姓名", canonical_account("B-姓名", lookup))
        self.assertEqual("B", account_group(canonical_account("B-账号1", lookup)))
        self.assertEqual("A", account_group(canonical_account("E-王志彬", {"E-王志彬": "A-王志彬"})))
        self.assertEqual("B-姓名,其他账号", canonical_data_owners("B-账号1、B-姓名,其他账号", lookup))
        self.assertEqual("无关账号，其他账号", canonical_data_owners("无关账号，其他账号", lookup))

    def test_ambiguous_maps_are_rejected(self):
        with self.assertRaises(ValueError):
            build_account_merge_lookup({"主账号1": ["账号"], "主账号2": ["账号"]})
        with self.assertRaises(ValueError):
            build_account_merge_lookup({"主账号1": ["主账号2"], "主账号2": ["账号"]})


if __name__ == "__main__":
    unittest.main()

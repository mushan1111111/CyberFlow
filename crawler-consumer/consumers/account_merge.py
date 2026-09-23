import re


def build_account_merge_lookup(user_merge_map: dict) -> dict[str, str]:
    if not isinstance(user_merge_map, dict):
        raise ValueError("user_merge_map must be an object")
    lookup: dict[str, str] = {}
    primaries: set[str] = set()
    for raw_primary, raw_aliases in user_merge_map.items():
        if not isinstance(raw_primary, str):
            raise ValueError("user_merge_map primary accounts must be strings")
        primary = raw_primary.strip()
        if not primary or primary in primaries or not isinstance(raw_aliases, list) or not raw_aliases:
            raise ValueError("user_merge_map contains an invalid primary account")
        primaries.add(primary)
        for raw_alias in raw_aliases:
            if not isinstance(raw_alias, str):
                raise ValueError("user_merge_map aliases must be strings")
            alias = raw_alias.strip()
            if not alias or alias == primary or alias in lookup:
                raise ValueError(f"user_merge_map contains an invalid or duplicate account: {alias}")
            lookup[alias] = primary
    if primaries.intersection(lookup):
        raise ValueError("a primary account cannot also be merged into another account")
    return lookup


def canonical_account(value, lookup: dict[str, str]) -> str:
    account = str(value or "").strip()
    return lookup.get(account, account)


def account_group(value) -> str | None:
    account = str(value or "").strip()
    if "-" not in account:
        return None
    prefix = account.split("-", 1)[0].strip()
    return prefix if prefix and len(prefix) <= 32 and prefix.replace("_", "").isalnum() else None


def canonical_data_owners(value, lookup: dict[str, str]) -> str:
    raw_value = str(value or "").strip()
    raw_owners = [item.strip() for item in re.split(r"[,，、\n]", raw_value) if item.strip()]
    if not any(owner in lookup for owner in raw_owners):
        return raw_value
    owners = [canonical_account(owner, lookup) for owner in raw_owners]
    return ",".join(dict.fromkeys(owner for owner in owners if owner))

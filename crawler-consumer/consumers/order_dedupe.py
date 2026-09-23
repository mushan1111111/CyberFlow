from loguru import logger

from consumers.order_identity import build_dedupe_keys


async def refresh_dedupe_keys(cur, user_group: str, order_day) -> None:
    """Rebuild transitive email-or-address identities for one group and business day."""
    await cur.execute(
        """SELECT id, user_group, create_time, product_host, shipping_email, shipping_address
           FROM orders
           WHERE user_group=%s AND create_time >= %s
             AND create_time < DATE_ADD(%s, INTERVAL 1 DAY)""",
        (user_group, order_day, order_day),
    )
    columns = [column[0] for column in cur.description]
    rows = [dict(zip(columns, row)) for row in await cur.fetchall()]
    keys = build_dedupe_keys(rows)
    if not keys:
        return
    await cur.executemany(
        "UPDATE orders SET dedupe_key=%s WHERE user_group=%s AND id=%s",
        [(key, group, order_id) for (group, order_id), key in keys.items()],
    )
    logger.info(
        f"🔗 Refreshed order identities: group={user_group}, day={order_day}, "
        f"rows={len(rows)}, deduplicated={len(set(keys.values()))}"
    )

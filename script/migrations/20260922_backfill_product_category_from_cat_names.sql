-- site/site/list returns the category as a JSON array (cat_names) and never
-- fills the single-value product_category column, so exports, keyword filters
-- and the "sites by category" grouping all ended up empty.  Mirror cat_names
-- into product_category for every row that has categories but no single value.
-- Idempotent: rows that already carry a product_category are left untouched.
USE cyberflow;
SET NAMES utf8mb4;

UPDATE site_info s
JOIN (
    SELECT s2.id, GROUP_CONCAT(jt.category_name ORDER BY jt.ord SEPARATOR '、') AS joined_category
    FROM site_info s2, JSON_TABLE(s2.cat_names, '$[*]' COLUMNS (ord FOR ORDINALITY, category_name VARCHAR(255) PATH '$')) jt
    WHERE s2.cat_names IS NOT NULL AND JSON_LENGTH(s2.cat_names) > 0
    GROUP BY s2.id
) derived ON derived.id = s.id
SET s.product_category = derived.joined_category
WHERE COALESCE(s.product_category, '') = '';

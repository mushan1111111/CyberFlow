-- Place shared custom-category maintenance inside the product collection module.
UPDATE sys_menu
SET parent_id=4, sort_order=3
WHERE id=70;

-- A role with category access also needs the parent directory to render it.
INSERT IGNORE INTO sys_role_menu(role_id, menu_id)
SELECT rm.role_id, 4
FROM sys_role_menu rm
WHERE rm.menu_id=70;


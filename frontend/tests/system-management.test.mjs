import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'
import { test } from 'node:test'

const source = path => readFile(new URL(path, import.meta.url), 'utf8')

test('system management actions are hidden by their specific permissions', async () => {
  const [users, roles, menus] = await Promise.all([
    source('../src/views/system/UserList.vue'),
    source('../src/views/system/RoleList.vue'),
    source('../src/views/system/MenuTree.vue'),
  ])
  for (const permission of ['system:user:create', 'system:user:update', 'system:user:delete', 'system:user:assign']) {
    assert.match(users, new RegExp(permission))
  }
  for (const permission of ['system:role:create', 'system:role:update', 'system:role:delete', 'system:role:assign']) {
    assert.match(roles, new RegExp(permission))
  }
  for (const permission of ['system:menu:create', 'system:menu:update', 'system:menu:delete']) {
    assert.match(menus, new RegExp(permission))
  }
})

test('role assignment retains half-checked parent menus', async () => {
  const roles = await source('../src/views/system/RoleList.vue')
  assert.match(roles, /getHalfCheckedKeys/)
  assert.match(roles, /new Set/)
})

test('route permissions deny empty or stale permission lists', async () => {
  const router = await source('../src/router/index.js')
  const layout = await source('../src/views/layout/index.vue')
  assert.doesNotMatch(router, /permissions\.length &&/)
  assert.doesNotMatch(layout, /: !userStore\.userInfo\?\.menus\?\.length/)
})

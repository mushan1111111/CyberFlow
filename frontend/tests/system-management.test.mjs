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

test('notification robots are protected, masked and testable from system management', async () => {
  const [view, api, router, layout] = await Promise.all([
    source('../src/views/system/NotificationConfig.vue'),
    source('../src/api/system.js'),
    source('../src/router/index.js'),
    source('../src/views/layout/index.vue'),
  ])
  assert.match(view, /system:notification:manage/)
  assert.match(view, /system:notification:test/)
  assert.match(view, /Webhook 与签名密钥会加密保存且不会回显/)
  assert.match(view, /DINGTALK/)
  assert.match(view, /FEISHU/)
  assert.match(view, /LARK/)
  assert.match(view, /messageTemplate/)
  assert.match(view, /\{\{title\}\}/)
  assert.match(view, /\{\{content\}\}/)
  assert.match(view, /最终预览/)
  assert.match(api, /notification\/\$\{id\}\/test/)
  assert.match(router, /system:notification:view/)
  assert.match(layout, /\/system\/notification/)
})

test('custom categories are grouped under product collection', async () => {
  const [layout, router] = await Promise.all([
    source('../src/views/layout/index.vue'),
    source('../src/router/index.js'),
  ])
  const productSection = layout.match(/menuName: '商品采集'[\s\S]*?menuName: '站点建设'/)?.[0] || ''
  assert.match(productSection, /menuName: '自定义分类'/)
  assert.doesNotMatch(layout, /\{ id:70, menuName:'自定义分类'/)
  assert.match(router, /title:'自定义分类', section:'商品采集'/)
})

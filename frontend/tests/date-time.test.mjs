import assert from 'node:assert/strict'
import { test } from 'node:test'
import { formatBeijingDateTime } from '../src/utils/dateTime.js'

test('UTC task timestamps without offsets display as Beijing time', () => {
  assert.equal(formatBeijingDateTime('2026-09-20T15:39:43'), '2026-09-20 23:39:43')
  assert.equal(formatBeijingDateTime('2026-09-20 16:30:00'), '2026-09-21 00:30:00')
})

test('timestamps with explicit offsets are converted without a second offset', () => {
  assert.equal(formatBeijingDateTime('2026-09-20T15:39:43Z'), '2026-09-20 23:39:43')
  assert.equal(formatBeijingDateTime('2026-09-20T23:39:43+08:00'), '2026-09-20 23:39:43')
})

test('missing or invalid timestamps use the supplied empty label', () => {
  assert.equal(formatBeijingDateTime(null), '时间未记录')
  assert.equal(formatBeijingDateTime('not-a-date', '暂无记录'), '暂无记录')
})


const BEIJING_TIME_ZONE = 'Asia/Shanghai'

const beijingDateTimeFormatter = new Intl.DateTimeFormat('zh-CN', {
  timeZone: BEIJING_TIME_ZONE,
  year: 'numeric',
  month: '2-digit',
  day: '2-digit',
  hour: '2-digit',
  minute: '2-digit',
  second: '2-digit',
  hourCycle: 'h23',
})

/**
 * Task timestamps are persisted as UTC DATETIME values and therefore arrive
 * without an offset. Explicit offsets from other APIs are preserved.
 */
function parseUtcDateTime(value) {
  if (value instanceof Date) return value
  const raw = String(value ?? '').trim()
  if (!raw) return null

  const iso = raw.replace(' ', 'T')
  const hasOffset = /(?:Z|[+-]\d{2}:?\d{2})$/i.test(iso)
  const parsed = new Date(hasOffset ? iso : `${iso}Z`)
  return Number.isNaN(parsed.getTime()) ? null : parsed
}

export function formatBeijingDateTime(value, empty = '时间未记录') {
  const parsed = parseUtcDateTime(value)
  if (!parsed) return empty

  const parts = Object.fromEntries(
    beijingDateTimeFormatter.formatToParts(parsed)
      .filter(part => part.type !== 'literal')
      .map(part => [part.type, part.value]),
  )
  return `${parts.year}-${parts.month}-${parts.day} ${parts.hour}:${parts.minute}:${parts.second}`
}

